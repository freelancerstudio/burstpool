/*
 * This file is generated by jOOQ.
 */
package burst.pool.db.tables;


import burst.pool.db.Indexes;
import burst.pool.db.Keys;
import burst.pool.db.Pooldb;
import burst.pool.db.tables.records.PoolstateRecord;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;


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
public class Poolstate extends TableImpl<PoolstateRecord> {

    private static final long serialVersionUID = 100558607;

    /**
     * The reference instance of <code>pooldb.poolstate</code>
     */
    public static final Poolstate POOLSTATE = new Poolstate();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PoolstateRecord> getRecordType() {
        return PoolstateRecord.class;
    }

    /**
     * The column <code>pooldb.poolstate.key</code>.
     */
    public final TableField<PoolstateRecord, String> KEY = createField("key", org.jooq.impl.SQLDataType.VARCHAR(50).nullable(false), this, "");

    /**
     * The column <code>pooldb.poolstate.value</code>.
     */
    public final TableField<PoolstateRecord, String> VALUE = createField("value", org.jooq.impl.SQLDataType.CLOB.defaultValue(org.jooq.impl.DSL.field("NULL", org.jooq.impl.SQLDataType.CLOB)), this, "");

    /**
     * Create a <code>pooldb.poolstate</code> table reference
     */
    public Poolstate() {
        this(DSL.name("poolstate"), null);
    }

    /**
     * Create an aliased <code>pooldb.poolstate</code> table reference
     */
    public Poolstate(String alias) {
        this(DSL.name(alias), POOLSTATE);
    }

    /**
     * Create an aliased <code>pooldb.poolstate</code> table reference
     */
    public Poolstate(Name alias) {
        this(alias, POOLSTATE);
    }

    private Poolstate(Name alias, Table<PoolstateRecord> aliased) {
        this(alias, aliased, null);
    }

    private Poolstate(Name alias, Table<PoolstateRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> Poolstate(Table<O> child, ForeignKey<O, PoolstateRecord> key) {
        super(child, key, POOLSTATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Pooldb.POOLDB;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.POOLSTATE_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<PoolstateRecord> getPrimaryKey() {
        return Keys.KEY_POOLSTATE_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<PoolstateRecord>> getKeys() {
        return Arrays.<UniqueKey<PoolstateRecord>>asList(Keys.KEY_POOLSTATE_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Poolstate as(String alias) {
        return new Poolstate(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Poolstate as(Name alias) {
        return new Poolstate(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Poolstate rename(String name) {
        return new Poolstate(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Poolstate rename(Name name) {
        return new Poolstate(name, null);
    }
}