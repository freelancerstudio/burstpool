/*
 * This file is generated by jOOQ.
 */
package burst.pool.db;


import burst.pool.db.tables.Bestsubmissions;
import burst.pool.db.tables.FlywaySchemaHistory;
import burst.pool.db.tables.Minerdeadlines;
import burst.pool.db.tables.Miners;
import burst.pool.db.tables.Payouts;
import burst.pool.db.tables.Poolstate;
import burst.pool.db.tables.Wonblocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.9"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Pooldb extends SchemaImpl {

    private static final long serialVersionUID = 2115849476;

    /**
     * The reference instance of <code>pooldb</code>
     */
    public static final Pooldb POOLDB = new Pooldb();

    /**
     * The table <code>pooldb.bestsubmissions</code>.
     */
    public final Bestsubmissions BESTSUBMISSIONS = burst.pool.db.tables.Bestsubmissions.BESTSUBMISSIONS;

    /**
     * The table <code>pooldb.flyway_schema_history</code>.
     */
    public final FlywaySchemaHistory FLYWAY_SCHEMA_HISTORY = burst.pool.db.tables.FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY;

    /**
     * The table <code>pooldb.minerdeadlines</code>.
     */
    public final Minerdeadlines MINERDEADLINES = burst.pool.db.tables.Minerdeadlines.MINERDEADLINES;

    /**
     * The table <code>pooldb.miners</code>.
     */
    public final Miners MINERS = burst.pool.db.tables.Miners.MINERS;

    /**
     * The table <code>pooldb.payouts</code>.
     */
    public final Payouts PAYOUTS = burst.pool.db.tables.Payouts.PAYOUTS;

    /**
     * The table <code>pooldb.poolstate</code>.
     */
    public final Poolstate POOLSTATE = burst.pool.db.tables.Poolstate.POOLSTATE;

    /**
     * The table <code>pooldb.wonblocks</code>.
     */
    public final Wonblocks WONBLOCKS = burst.pool.db.tables.Wonblocks.WONBLOCKS;

    /**
     * No further instances allowed
     */
    private Pooldb() {
        super("pooldb", null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        List result = new ArrayList();
        result.addAll(getTables0());
        return result;
    }

    private final List<Table<?>> getTables0() {
        return Arrays.<Table<?>>asList(
            Bestsubmissions.BESTSUBMISSIONS,
            FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY,
            Minerdeadlines.MINERDEADLINES,
            Miners.MINERS,
            Payouts.PAYOUTS,
            Poolstate.POOLSTATE,
            Wonblocks.WONBLOCKS);
    }
}
