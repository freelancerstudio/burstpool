package burst.pool.pool;

import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.MiningInfo;
import burst.kit.service.BurstNodeService;
import burst.pool.brs.Generator;
import burst.pool.miners.MinerTracker;
import burst.pool.storage.config.PropertyService;
import burst.pool.storage.config.Props;
import burst.pool.storage.persistent.StorageService;
import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Pool {
    private static final Logger logger = LoggerFactory.getLogger(Pool.class);

    private final BurstNodeService nodeService;
    private final BurstCrypto burstCrypto = BurstCrypto.getInstance();
    private final StorageService storageService;
    private final PropertyService propertyService;
    private final MinerTracker minerTracker;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final Semaphore processBlockSemaphore = new Semaphore(1);
    private final Semaphore resetRoundSemaphore = new Semaphore(1);
    private final Semaphore processDeadlineSemaphore = new Semaphore(1);

    // Variables
    private final AtomicReference<Instant> roundStartTime = new AtomicReference<>();
    private final AtomicReference<Submission> bestDeadline = new AtomicReference<>();
    private final AtomicReference<MiningInfo> miningInfo = new AtomicReference<>();
    private final Set<BurstAddress> myRewardRecipients = new HashSet<>();

    public Pool(BurstNodeService nodeService, StorageService storageService, PropertyService propertyService, MinerTracker minerTracker) {
        this.storageService = storageService;
        this.minerTracker = minerTracker;
        this.propertyService = propertyService;
        this.nodeService = nodeService;
        disposables.add(refreshMiningInfoThread());
        disposables.add(processBlocksThread());
        resetRound(null);
    }

    private Disposable processBlocksThread() {
        return Observable.interval(0, 1, TimeUnit.MILLISECONDS)
                .flatMapCompletable(l -> processNextBlock().onErrorComplete(e -> {
                    onProcessBlocksError(e, false);
                    return true;
                }))
                .subscribe(() -> {}, e -> onProcessBlocksError(e, true));
    }

    private void onProcessBlocksError(Throwable throwable, boolean fatal) {
        if (fatal) {
            logger.error("Fatal error processing blocks (Thread now shutdown)", throwable);
        } else {
            logger.warn("Non-fatal error processing blocks", throwable);
        }
    }

    private Disposable refreshMiningInfoThread() {
        return nodeService.getMiningInfo()
                .subscribe(this::onMiningInfo, e -> onMiningInfoError(e, true));
    }

    private void onMiningInfo(MiningInfo newMiningInfo) {
        if (miningInfo.get() == null || !Arrays.equals(miningInfo.get().getGenerationSignature(), newMiningInfo.getGenerationSignature())
                || !Objects.equals(miningInfo.get().getHeight(), newMiningInfo.getHeight())) {
            logger.info("NEW BLOCK (block " + newMiningInfo.getHeight() + ", gensig " + Hex.toHexString(newMiningInfo.getGenerationSignature()) +", diff " + newMiningInfo.getBaseTarget() + ")");
            resetRound(newMiningInfo);
        }
    }

    private void onMiningInfoError(Throwable throwable, boolean fatal) {
        if (fatal) {
            logger.error("Fatal error fetching mining info (Thread now shutdown)", throwable);
        } else {
            logger.warn("Non-fatal error fetching mining info", throwable);
        }
    }

    private Completable processNextBlock() {
        if (miningInfo.get() == null || processBlockSemaphore.availablePermits() == 0 || miningInfo.get().getHeight() - 1 <= storageService.getLastProcessedBlock() + propertyService.getInt(Props.processLag)) {
            return Completable.complete();
        }

        try {
            processBlockSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        StorageService transactionalStorageService;
        try {
            transactionalStorageService = storageService.beginTransaction();
        } catch (Exception e) {
            logger.error("Could not open transactional storage service", e);
            processBlockSemaphore.release();
            return Completable.complete();
        }

        minerTracker.setCurrentlyProcessingBlock(true);

        List<Long> fastBlocks = new ArrayList<>();
        transactionalStorageService.getBestSubmissions().forEach((height, deadline) -> {
            if (deadline.getDeadline() < propertyService.getInt(Props.tMin)) {
                fastBlocks.add(height);
            }
        });

        if (transactionalStorageService.getBestSubmissionForBlock(transactionalStorageService.getLastProcessedBlock() + 1) == null) {
            onProcessedBlock(transactionalStorageService);
            return Completable.complete();
        }

        return nodeService.getBlock(transactionalStorageService.getLastProcessedBlock() + 1)
                .flatMapCompletable(block -> Completable.fromAction(() -> {
                    Submission submission = transactionalStorageService.getBestSubmissionForBlock(block.getHeight());
                    if (submission != null && Objects.equals(block.getGenerator(), submission.getMiner()) && Objects.equals(block.getNonce(), submission.getNonce())) {
                        minerTracker.onBlockWon(transactionalStorageService, transactionalStorageService.getLastProcessedBlock() + 1, block.getId(), block.getNonce(), block.getGenerator(), new BurstValue(block.getBlockReward().add(block.getTotalFee())), fastBlocks);
                    } else {
                        minerTracker.onBlockNotWon(transactionalStorageService, transactionalStorageService.getLastProcessedBlock() + 1, fastBlocks);
                    }
                }))
                .doOnComplete(() -> onProcessedBlock(transactionalStorageService))
                .onErrorComplete(t -> {
                    logger.warn("Error processing block " + transactionalStorageService.getLastProcessedBlock() + 1, t);
                    try {
                        transactionalStorageService.rollbackTransaction();
                        transactionalStorageService.close();
                    } catch (Exception e) {
                        logger.error("Error rolling back transaction", e);
                    }
                    minerTracker.setCurrentlyProcessingBlock(false);
                    processBlockSemaphore.release();
                    return true;
                });
    }

    private void onProcessedBlock(StorageService transactionalStorageService) {
        // TODO this needs to be done if block is behind nAvg otherwise fast block calculation breaks
        //storageService.removeBestSubmission(storageService.getLastProcessedBlock() + 1);
        transactionalStorageService.incrementLastProcessedBlock();
        try {
            transactionalStorageService.commitTransaction();
            transactionalStorageService.close();
        } catch (Exception e) {
            logger.error("Error committing transaction", e);
        }
        minerTracker.setCurrentlyProcessingBlock(false);
        processBlockSemaphore.release();
        minerTracker.payoutIfNeeded(storageService);
    }

    private void resetRound(MiningInfo newMiningInfo) {
        // Traffic flow - we want to stop new requests but let old ones finish before we go ahead.
        try {
            // Lock the reset round semaphore to stop accepting new requests
            resetRoundSemaphore.acquire();
            // Wait for all requests to be processed
            while (processDeadlineSemaphore.getQueueLength() > 0) {
                Thread.sleep(1);
            }
            // Lock the process block semaphore as we are going to be modifying bestDeadline
            processDeadlineSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        bestDeadline.set(null);
        disposables.add(nodeService.getAccountsWithRewardRecipient(burstCrypto.getBurstAddressFromPassphrase(propertyService.getString(Props.passphrase)))
                .subscribe(this::onRewardRecipients, this::onRewardRecipientsError));
        roundStartTime.set(Instant.now());
        miningInfo.set(newMiningInfo);
        // Unlock to signal we have finished modifying bestDeadline
        processDeadlineSemaphore.release();
        // Unlock to start accepting requests again
        resetRoundSemaphore.release();
    }

    BigInteger checkNewSubmission(Submission submission, String userAgent) throws SubmissionException {
        if (miningInfo.get() == null) {
            throw new SubmissionException("Pool does not have mining info");
        }

        if (!myRewardRecipients.contains(submission.getMiner())) {
            throw new SubmissionException("Reward recipient not set to pool");
        }

        // If we are resetting the request must be for the previous round and no longer matters - reject
        if (resetRoundSemaphore.availablePermits() < 0) {
            throw new SubmissionException("Cannot submit - new round starting");
        }

        try {
            processDeadlineSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SubmissionException("Server Interrupted");
        }

        try {
            BigInteger deadline = Generator.calcDeadline(miningInfo.get(), submission);

            if (deadline.compareTo(BigInteger.valueOf(propertyService.getLong(Props.maxDeadline))) >= 0) {
                throw new SubmissionException("Deadline exceeds maximum allowed deadline");
            }

            logger.debug("New submission from " + submission.getMiner() + " of nonce " + submission.getNonce() + ", calculated deadline " + deadline.toString() + " seconds.");

            if (bestDeadline.get() != null) {
                logger.debug("Best deadline is " + Generator.calcDeadline(miningInfo.get(), bestDeadline.get()) + ", new deadline is " + deadline);
                if (deadline.compareTo(Generator.calcDeadline(miningInfo.get(), bestDeadline.get())) < 0) {
                    logger.debug("Newer deadline is better! Submitting...");
                    onNewBestDeadline(miningInfo.get().getHeight(), submission);
                }
            } else {
                logger.debug("This is the first deadline, submitting...");
                onNewBestDeadline(miningInfo.get().getHeight(), submission);
            }

            minerTracker.onMinerSubmittedDeadline(storageService, submission.getMiner(), deadline, BigInteger.valueOf(miningInfo.get().getBaseTarget()), miningInfo.get().getHeight(), userAgent);

            return deadline;
        } finally {
            processDeadlineSemaphore.release();
        }
    }

    private void onNewBestDeadline(long blockHeight, Submission submission) throws SubmissionException {
        bestDeadline.set(submission);
        submitDeadline(submission);
        storageService.setOrUpdateBestSubmissionForBlock(blockHeight, new StoredSubmission(submission.getMiner(), submission.getNonce(), Generator.calcDeadline(miningInfo.get(), submission).longValue()));
    }

    private void submitDeadline(Submission submission) {
        disposables.add(nodeService.submitNonce(propertyService.getString(Props.passphrase), submission.getNonce().toString(), submission.getMiner().getBurstID()) // TODO burstkit4j accept nonce as bigint
                .retry(propertyService.getInt(Props.submitNonceRetryCount))
                .subscribe(this::onNonceSubmitted, this::onSubmitNonceError));
    }

    private void onRewardRecipients(BurstAddress[] rewardRecipients) {
        myRewardRecipients.clear();
        myRewardRecipients.addAll(Arrays.asList(rewardRecipients));
    }

    private void onRewardRecipientsError(Throwable t) {
        logger.error("Error fetching pool's reward recipients", t);
    }

    private void onNonceSubmitted(long deadline) {
        logger.debug("Submitted nonce to node. Deadline is " + Long.toUnsignedString(deadline));
    }

    private void onSubmitNonceError(Throwable t) {
        logger.error("Error submitting nonce to node", t);
    }

    MiningInfo getMiningInfo() {
        return miningInfo.get();
    }

    public JsonObject getCurrentRoundInfo(Gson gson) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("roundStart", roundStartTime.get().getEpochSecond());
        jsonObject.addProperty("roundElapsed", Instant.now().getEpochSecond() - roundStartTime.get().getEpochSecond());
        if (bestDeadline.get() != null) {
            JsonObject bestDeadlineJson = new JsonObject();
            bestDeadlineJson.addProperty("miner", bestDeadline.get().getMiner().getID());
            bestDeadlineJson.addProperty("minerRS", bestDeadline.get().getMiner().getFullAddress());
            bestDeadlineJson.addProperty("nonce", bestDeadline.get().getNonce());
            try {
                bestDeadlineJson.addProperty("deadline", Generator.calcDeadline(miningInfo.get(), bestDeadline.get()));
            } catch (SubmissionException ignored) {
            }
            jsonObject.add("bestDeadline", bestDeadlineJson);
        } else {
            jsonObject.add("bestDeadline", JsonNull.INSTANCE);
        }
        jsonObject.add("miningInfo", gson.toJsonTree(miningInfo.get()));
        return jsonObject;
    }

    public BurstAddress getAccount() {
        return burstCrypto.getBurstAddressFromPassphrase(propertyService.getString(Props.passphrase));
    }
}
