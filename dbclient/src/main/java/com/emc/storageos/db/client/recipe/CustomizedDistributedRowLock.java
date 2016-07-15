/*
 * Copyright (c) 2008-2014 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.db.client.recipe;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.cassandra.serializers.LongSerializer;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.emc.storageos.db.client.impl.ColumnFamilyDefinition;
import com.emc.storageos.db.client.impl.DbClientContext;
import com.emc.storageos.db.client.impl.RowMutator;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netflix.astyanax.retry.RetryPolicy;
import com.netflix.astyanax.retry.RunOnce;
import com.netflix.astyanax.util.TimeUUIDUtils;

/**
 * Internal DistributedRowLock to acquiring and release a row lock
 *
 * Usage:
 *
 * CustomizedDistributedRowLock lock = new CustomizedDistributedRowLock(...); try {
 * lock.acquire(); // Do something ... } catch (BusyLockException) { // The lock
 * was already taken by a different process } catch (StaleLockException) { //
 * The row has a stale lock that needs to be addressed // This is usually caused
 * when no column TTL is set and the client // crashed before releasing the
 * lock. The DistributedRowLock should // have the option to auto delete stale
 * locks. } finally { lock.release(); }
 *
 */
public class CustomizedDistributedRowLock<K> {
    public static final int LOCK_TIMEOUT = 60;
    public static final TimeUnit DEFAULT_OPERATION_TIMEOUT_UNITS = TimeUnit.MINUTES;
    public static final String DEFAULT_LOCK_PREFIX = "_LOCK_";

    private final ColumnFamilyDefinition columnFamily; // The column family for data and lock
    private final K key;                       // Key being locked
    private final DbClientContext context;

    private long timeout = LOCK_TIMEOUT;                   // Timeout after which the lock expires. Units defined by timeoutUnits.
    private TimeUnit timeoutUnits = DEFAULT_OPERATION_TIMEOUT_UNITS;
    private String prefix = DEFAULT_LOCK_PREFIX;            // Prefix to identify the lock columns
    private ConsistencyLevel consistencyLevel = ConsistencyLevel.LOCAL_QUORUM;
    private boolean failOnStaleLock = false;
    private String lockColumn = null;
    private String lockId = null;
    private Set<String> locksToDelete = Sets.newHashSet();
    private Map<String, ByteBuffer> columns = null;
    private Integer ttl = null;                           // Units in seconds
    private boolean readDataColumns = false;
    private RetryPolicy backoffPolicy = RunOnce.get();
    private long acquireTime = 0;
    private int retryCount = 0;

    public CustomizedDistributedRowLock(DbClientContext context, ColumnFamilyDefinition columnFamily, K key) {
        this.context = context;
        this.columnFamily = columnFamily;
        this.key = key;
        this.lockId = TimeUUIDUtils.getUniqueTimeUUIDinMicros().toString();
    }

    /**
     * Modify the consistency level being used. Consistency should always be a
     * variant of quorum. The default is CL_QUORUM, which is OK for single
     * region. For multi region the consistency level should be CL_LOCAL_QUORUM.
     * CL_EACH_QUORUM can be used but will Incur substantial latency.
     * 
     * @param consistencyLevel
     */
    public CustomizedDistributedRowLock<K> withConsistencyLevel(ConsistencyLevel consistencyLevel) {
        this.consistencyLevel = consistencyLevel;
        return this;
    }

    /**
     * Specify the prefix that uniquely distinguishes the lock columns from data
     * column
     * 
     * @param prefix
     */
    public CustomizedDistributedRowLock<K> withColumnPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * If true the first read will also fetch all the columns in the row as
     * opposed to just the lock columns.
     * 
     * @param flag
     */
    public CustomizedDistributedRowLock<K> withDataColumns(boolean flag) {
        this.readDataColumns = flag;
        return this;
    }

    /**
     * Override the autogenerated lock column.
     * 
     * @param lockId
     */
    public CustomizedDistributedRowLock<K> withLockId(String lockId) {
        this.lockId = lockId;
        return this;
    }

    /**
     * When set to true the operation will fail if a stale lock is detected
     * 
     * @param failOnStaleLock
     */
    public CustomizedDistributedRowLock<K> failOnStaleLock(boolean failOnStaleLock) {
        this.failOnStaleLock = failOnStaleLock;
        return this;
    }

    /**
     * Time for failed locks. Under normal circumstances the lock column will be
     * deleted. If not then this lock column will remain and the row will remain
     * locked. The lock will expire after this timeout.
     * 
     * @param timeout
     * @param unit
     */
    public CustomizedDistributedRowLock<K> expireLockAfter(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.timeoutUnits = unit;
        return this;
    }

    /**
     * This is the TTL on the lock column being written, as opposed to expireLockAfter which
     * is written as the lock column value. Whereas the expireLockAfter can be used to
     * identify a stale or abandoned lock the TTL will result in the stale or abandoned lock
     * being eventually deleted by cassandra. Set the TTL to a number that is much greater
     * tan the expireLockAfter time.
     * 
     * @param ttl
     */
    public CustomizedDistributedRowLock<K> withTtl(Integer ttl) {
        this.ttl = ttl;
        return this;
    }

    public CustomizedDistributedRowLock<K> withTtl(Integer ttl, TimeUnit units) {
        this.ttl = (int) TimeUnit.SECONDS.convert(ttl, units);
        return this;
    }

    public CustomizedDistributedRowLock<K> withBackoff(RetryPolicy policy) {
        this.backoffPolicy = policy;
        return this;
    }

    /**
     * Try to take the lock. The caller must call .release() to properly clean up
     * the lock columns from cassandra
     * 
     * @throws Exception
     */
    public void acquire() throws Exception {

        Preconditions.checkArgument(ttl == null || TimeUnit.SECONDS.convert(timeout, timeoutUnits) < ttl, "Timeout " + timeout
                + " must be less than TTL " + ttl);

        RetryPolicy retry = backoffPolicy.duplicate();
        retryCount = 0;
        while (true) {
            try {
                long curTimeMicros = getCurrentTimeMicros();

                RowMutator mutator = new RowMutator(context);
                mutator.setWriteCL(consistencyLevel);
                fillLockMutation(mutator, curTimeMicros, ttl);
                mutator.execute();

                verifyLock(curTimeMicros);
                acquireTime = System.currentTimeMillis();
                return;
            } catch (BusyLockException e) {
                release();
                if (!retry.allowRetry()) {
                    throw e;
                }
                retryCount++;
            }
        }
    }

    /**
     * Take the lock and return the row data columns. Use this, instead of acquire, when you
     * want to implement a read-modify-write scenario and want to reduce the number of calls
     * to Cassandra.
     * 
     * @throws Exception
     */
    public Map<String, ByteBuffer> acquireLockAndReadRow() throws Exception {
        withDataColumns(true);
        acquire();
        return getDataColumns();
    }

    /**
     * Verify that the lock was acquired. This shouldn't be called unless it's part of a recipe
     * built on top of CustomizedDistributedRowLock.
     * 
     * @param curTimeInMicros
     * @throws BusyLockException
     */
    public void verifyLock(long curTimeInMicros) throws Exception, BusyLockException, StaleLockException {
        if (lockColumn == null) {
            throw new IllegalStateException("verifyLock() called without attempting to take the lock");
        }

        // Read back all columns. There should be only 1 if we got the lock
        Map<String, Long> lockResult = readLockColumns(readDataColumns);

        // Cleanup and check that we really got the lock
        for (Entry<String, Long> entry : lockResult.entrySet()) {
            // This is a stale lock that was never cleaned up
            if (entry.getValue() != 0 && curTimeInMicros > entry.getValue()) {
                if (failOnStaleLock) {
                    throw new StaleLockException("Stale lock on row '" + key + "'.  Manual cleanup requried.");
                }
                locksToDelete.add(entry.getKey());
            }
            // Lock already taken, and not by us
            else if (!entry.getKey().equals(lockColumn)) {
                throw new BusyLockException("Lock already acquired for row '" + key + "' with lock column " + entry.getKey());
            }
        }
    }

    /**
     * Release the lock by releasing this and any other stale lock columns
     */
    public void release() throws Exception {
        if (!locksToDelete.isEmpty() || lockColumn != null) {
            RowMutator mutator = new RowMutator(context);
            mutator.setWriteCL(consistencyLevel);
            fillReleaseMutation(mutator, false);
            mutator.execute();
        }
    }

    /**
     * Release using the provided mutation. Use this when you want to commit actual data
     * when releasing the lock
     * 
     * @param m
     * @throws Exception
     */
    public void releaseWithMutation(RowMutator mutator) throws Exception {
        releaseWithMutation(mutator, false);
    }

    public boolean releaseWithMutation(RowMutator mutator, boolean force) throws Exception {
        long elapsed = System.currentTimeMillis() - acquireTime;
        boolean isStale = false;
        if (timeout > 0 && elapsed > TimeUnit.MILLISECONDS.convert(timeout, this.timeoutUnits)) {
            isStale = true;
            if (!force) {
                throw new StaleLockException("Lock for '" + getKey() + "' became stale");
            }
        }

        mutator.setWriteCL(consistencyLevel);
        fillReleaseMutation(mutator, false);
        mutator.execute();

        return isStale;
    }

    /**
     * Return a mapping of existing lock columns and their expiration times
     * 
     * @throws Exception
     */
    public Map<String, Long> readLockColumns() throws Exception {
        return readLockColumns(false);
    }

    /**
     * Read all the lock columns. Will also ready data columns if withDataColumns(true) was called
     * 
     * @param readDataColumns
     * @return
     * @throws Exception
     */
    private Map<String, Long> readLockColumns(boolean readDataColumns) throws Exception {
        Map<String, Long> result = Maps.newLinkedHashMap();

        ConsistencyLevel read_consistencyLevel = consistencyLevel;
        // CASSANDRA actually does not support EACH_QUORUM for read which is meaningless as well.
        if (consistencyLevel == ConsistencyLevel.EACH_QUORUM) {
            read_consistencyLevel = ConsistencyLevel.LOCAL_QUORUM;
        }

        // Read all the columns
        if (readDataColumns) {
            columns = new HashMap<>();
            String queryString = String.format("select * from \"%s\" where key=?", columnFamily.getName());
            BoundStatement statement = context.getSession().prepare(queryString).bind(key);
            statement.setConsistencyLevel(read_consistencyLevel);
            ResultSet resultSet = context.getSession().execute(statement);

            for (Row row : resultSet) {
                String column1 = row.getString(1);
                if (column1.startsWith(prefix)) {
                    result.put(column1, LongSerializer.instance.deserialize(row.getBytes(2)));
                } else {
                    columns.put(column1, row.getBytes(2));
                }
            }
        }
        // Read only the lock columns
        else {
            String queryString = String.format("select * from \"%s\" where key=? and column1=?", columnFamily.getName());
            BoundStatement statement = context.getSession().prepare(queryString).bind(key, prefix);
            statement.setConsistencyLevel(read_consistencyLevel);
            ResultSet resultSet = context.getSession().execute(statement);

            for (Row row : resultSet) {
                result.put(row.getString(1), LongSerializer.instance.deserialize(row.getBytes(2)));
            }

        }
        return result;
    }

    /**
     * Release all locks. Use this carefully as it could release a lock for a
     * running operation.
     * 
     * @return Map of previous locks
     * @throws Exception
     */
    public Map<String, Long> releaseAllLocks() throws Exception {
        return releaseLocks(true);
    }

    /**
     * Release all expired locks for this key.
     * 
     * @return map of expire locks
     * @throws Exception
     */
    public Map<String, Long> releaseExpiredLocks() throws Exception {
        return releaseLocks(false);
    }

    /**
     * Delete locks columns. Set force=true to remove locks that haven't
     * expired yet.
     * 
     * This operation first issues a read to cassandra and then deletes columns
     * in the response.
     * 
     * @param force - Force delete of non expired locks as well
     * @return Map of locks released
     * @throws Exception
     */
    public Map<String, Long> releaseLocks(boolean force) throws Exception {
        Map<String, Long> locksToDelete = readLockColumns();

        RowMutator mutator = new RowMutator(context);
        mutator.setWriteCL(consistencyLevel);
        long now = getCurrentTimeMicros();
        for (Entry<String, Long> c : locksToDelete.entrySet()) {
            if (force || (c.getValue() > 0 && c.getValue() < now)) {
                mutator.deleteGlobalLockRecord(columnFamily.getName(), (String) key, c.getKey());
            }
        }
        mutator.execute();

        return locksToDelete;
    }

    /**
     * Get the current system time
     */
    private static long getCurrentTimeMicros() {
        return TimeUnit.MICROSECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Fill a mutation with the lock column. This may be used when the mutation
     * is executed externally but should be used with extreme caution to ensure
     * the lock is properly released
     * 
     * @param mutator
     * @param time
     * @param ttl
     * @return
     */
    public String fillLockMutation(RowMutator mutator, Long time, Integer ttl) {
        if (lockColumn != null) {
            if (!lockColumn.equals(prefix + lockId)) {
                throw new IllegalStateException("Can't change prefix or lockId after acquiring the lock");
            }
        }
        else {
            lockColumn = prefix + lockId;
        }

        Long timeoutValue = (time == null)
                ? Long.valueOf(0)
                : time + TimeUnit.MICROSECONDS.convert(timeout, timeoutUnits);

        //todo actually the 'key' is always being String, after remove columnFamily, we can remove <K>
        mutator.insertGlobalLockRecord(columnFamily.getName(), (String)key, lockColumn, timeoutValue, ttl);
        return lockColumn;
    }

    /**
     * Fill a mutation that will release the locks. This may be used from a
     * separate recipe to release multiple locks.
     * 
     * @param mutator
     * @param excludeCurrentLock
     */
    public void fillReleaseMutation(RowMutator mutator, boolean excludeCurrentLock) {
        // Add the deletes to the end of the mutation
        for (String c : locksToDelete) {
            mutator.deleteGlobalLockRecord(columnFamily.getName(), (String) key, c);
        }
        if (!excludeCurrentLock && lockColumn != null) {
            mutator.deleteGlobalLockRecord(columnFamily.getName(), (String) key, lockColumn);
        }
        locksToDelete.clear();
        lockColumn = null;
    }

    public Map<String, ByteBuffer> getDataColumns() {
        return columns;
    }

    public K getKey() {
        return key;
    }

    public ConsistencyLevel getConsistencyLevel() {
        return consistencyLevel;
    }

    public String getLockColumn() {
        return lockColumn;
    }

    public String getLockId() {
        return lockId;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getRetryCount() {
        return retryCount;
    }
    
    public class BusyLockException extends Exception {
        public BusyLockException(Exception e) {
            super(e);
        }

        public BusyLockException(String message, Exception e) {
            super(message, e);
        }

        public BusyLockException(String message) {
            super(message);
        }
    }

    public class StaleLockException extends Exception {
        public StaleLockException(Exception e) {
            super(e);
        }

        public StaleLockException(String message, Exception e) {
            super(message, e);
        }

        public StaleLockException(String message) {
            super(message);
        }
    }

}
