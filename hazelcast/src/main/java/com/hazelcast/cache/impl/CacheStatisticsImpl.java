/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.cache.impl;

import static com.hazelcast.internal.util.ConcurrencyUtil.setMax;

import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.LongAccumulator;

import com.hazelcast.cache.CacheStatistics;
import com.hazelcast.internal.monitor.impl.LocalReplicationStatsImpl;
import com.hazelcast.internal.tpcengine.util.ReflectionUtil;
import com.hazelcast.nearcache.NearCacheStats;

/**
 * {@link CacheStatistics} implementation for {@link com.hazelcast.cache.ICache}.
 *
 * <p>Statistics accumulated in this data object is published through MxBean as defined by spec.</p>
 *
 * @see com.hazelcast.cache.impl.CacheMXBeanImpl
 */
public class CacheStatisticsImpl
        implements CacheStatistics {

    protected static final float FLOAT_HUNDRED = 100.0f;
    protected static final long NANOSECONDS_IN_A_MICROSECOND = 1000L;

    protected static final VarHandle LAST_ACCESS_TIME = ReflectionUtil.findVarHandle("lastAccessTime", long.class);
    protected static final VarHandle LAST_UPDATE_TIME = ReflectionUtil.findVarHandle("lastUpdateTime", long.class);
    protected static final AtomicLongFieldUpdater<CacheStatisticsImpl> REMOVALS =
            AtomicLongFieldUpdater.newUpdater(CacheStatisticsImpl.class, "removals");
    protected static final AtomicLongFieldUpdater<CacheStatisticsImpl> EXPIRIES =
            AtomicLongFieldUpdater.newUpdater(CacheStatisticsImpl.class, "expiries");
    protected static final AtomicLongFieldUpdater<CacheStatisticsImpl> PUTS =
            AtomicLongFieldUpdater.newUpdater(CacheStatisticsImpl.class, "puts");
    protected static final AtomicLongFieldUpdater<CacheStatisticsImpl> EVICTIONS =
            AtomicLongFieldUpdater.newUpdater(CacheStatisticsImpl.class, "evictions");
    protected static final VarHandle PUT_TIME_TAKEN_NANOS = ReflectionUtil.findVarHandle("putTimeTakenNanos", long.class);
    protected static final VarHandle GET_CACHE_TIME_TAKEN_NANOS =
            ReflectionUtil.findVarHandle("getCacheTimeTakenNanos", long.class);
    protected static final VarHandle REMOVE_TIME_TAKEN_NANOS = ReflectionUtil.findVarHandle("removeTimeTakenNanos", long.class);

    /**
     * This field is not mutated (read only) so no need to define it as volatile.
     */
    protected long creationTime;

    protected volatile long lastAccessTime;
    protected volatile long lastUpdateTime;
    protected volatile long removals;
    protected volatile long expiries;
    protected volatile long puts;
    protected final LongAccumulator hits = new LongAccumulator(Long::sum, 0);
    protected final LongAccumulator misses = new LongAccumulator(Long::sum, 0);
    protected volatile long evictions;
    protected volatile long putTimeTakenNanos;
    protected volatile long getCacheTimeTakenNanos;
    protected volatile long removeTimeTakenNanos;

    protected final CacheEntryCountResolver cacheEntryCountResolver;

    private final LocalReplicationStatsImpl replicationStats = new LocalReplicationStatsImpl();

    public CacheStatisticsImpl(long creationTime) {
        this(creationTime, null);
    }

    public CacheStatisticsImpl(long creationTime, CacheEntryCountResolver cacheEntryCountResolver) {
        this.creationTime = creationTime;
        this.cacheEntryCountResolver = cacheEntryCountResolver;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Override
    public long getOwnedEntryCount() {
        if (cacheEntryCountResolver != null) {
            return cacheEntryCountResolver.getEntryCount();
        } else {
            return 0L;
        }
    }

    @Override
    public long getCacheRemovals() {
        return removals;
    }

    /**
     * The total number of expiries from the cache. An expiry may or may not be evicted.
     * This number represents the entries that fail evaluation and may not include the entries which are not yet
     * evaluated for expiry or not accessed.
     *
     * @return the number of expiries.
     */
    public long getCacheExpiries() {
        return expiries;
    }

    @Override
    public long getCacheGets() {
        return getCacheHits() + getCacheMisses();
    }

    @Override
    public long getCachePuts() {
        return puts;
    }

    @Override
    public long getCacheHits() {
        return hits.longValue();
    }

    @Override
    public long getCacheMisses() {
        return misses.longValue();
    }

    @Override
    public long getCacheEvictions() {
        return evictions;
    }

    public long getCachePutTimeTakenNanos() {
        return putTimeTakenNanos;
    }

    public long getCacheGetTimeTakenNanos() {
        return getCacheTimeTakenNanos;
    }

    public long getCacheRemoveTimeTakenNanos() {
        return removeTimeTakenNanos;
    }

    @Override
    public float getCacheHitPercentage() {
        final long cacheHits = getCacheHits();
        final long cacheGets = getCacheGets();
        if (cacheHits == 0 || cacheGets == 0) {
            return 0;
        }
        return (float) cacheHits / cacheGets * FLOAT_HUNDRED;
    }

    @Override
    public float getCacheMissPercentage() {
        final long cacheMisses = getCacheMisses();
        final long cacheGets = getCacheGets();
        if (cacheMisses == 0 || cacheGets == 0) {
            return 0;
        }
        return (float) cacheMisses / cacheGets * FLOAT_HUNDRED;
    }

    @Override
    public float getAverageGetTime() {
        final long cacheGetTimeTakenNanos = getCacheGetTimeTakenNanos();
        final long cacheGets = getCacheGets();
        if (cacheGetTimeTakenNanos == 0 || cacheGets == 0) {
            return 0;
        }
        return ((1f * cacheGetTimeTakenNanos) / cacheGets) / NANOSECONDS_IN_A_MICROSECOND;
    }

    @Override
    public float getAveragePutTime() {
        final long cachePutTimeTakenNanos = getCachePutTimeTakenNanos();
        final long cachePuts = getCachePuts();
        if (cachePutTimeTakenNanos == 0 || cachePuts == 0) {
            return 0;
        }
        return ((1f * cachePutTimeTakenNanos) / cachePuts) / NANOSECONDS_IN_A_MICROSECOND;
    }

    @Override
    public float getAverageRemoveTime() {
        final long cacheRemoveTimeTakenNanos = getCacheRemoveTimeTakenNanos();
        final long cacheRemoves = getCacheRemovals();
        if (cacheRemoveTimeTakenNanos == 0 || cacheRemoves == 0) {
            return 0;
        }
        return ((1f * cacheRemoveTimeTakenNanos) / cacheRemoves) / NANOSECONDS_IN_A_MICROSECOND;
    }

    @Override
    public LocalReplicationStatsImpl getReplicationStats() {
        return replicationStats;
    }

    /**
     * Implementation of {@link javax.cache.management.CacheStatisticsMXBean#clear()}.
     *
     * @see javax.cache.management.CacheStatisticsMXBean#clear()
     */
    public void clear() {
        // TODO Should we clear `creationTime` also? In fact, it doesn't make sense
        // TODO Should we clear `ownedEntryCount` also? In fact, it doesn't make sense

        puts = 0;
        misses.reset();
        removals = 0;
        expiries = 0;
        hits.reset();
        evictions = 0;
        getCacheTimeTakenNanos = 0;
        putTimeTakenNanos = 0;
        removeTimeTakenNanos = 0;
    }

    /**
     * Sets the cache last access time as atomic if the given time is bigger than it.
     *
     * @param time time to set the cache last access time
     */
    public void setLastAccessTime(long time) {
        setMax(this, LAST_ACCESS_TIME, time);
    }

    /**
     * Sets the cache last update time as atomic if the given time is bigger than it.
     *
     * @param time time to set the cache last update time
     */
    public void setLastUpdateTime(long time) {
        setMax(this, LAST_UPDATE_TIME, time);
    }

    /**
     * Increases the counter by `1`.
     */
    public void increaseCacheRemovals() {
        REMOVALS.incrementAndGet(this);
        setLastUpdateTime(System.currentTimeMillis());
    }

    /**
     * Increases the counter by the number specified.
     *
     * @param number the number by which the counter is increased.
     */
    public void increaseCacheRemovals(long number) {
        REMOVALS.addAndGet(this, number);
        setLastUpdateTime(System.currentTimeMillis());
    }

    /**
     * Increases the counter by `1`.
     */
    public void increaseCacheExpiries() {
        EXPIRIES.incrementAndGet(this);
        setLastUpdateTime(System.currentTimeMillis());
    }

    /**
     * Increases the counter by the number specified.
     *
     * @param number the number by which the counter is increased.
     */
    public void increaseCacheExpiries(long number) {
        EXPIRIES.addAndGet(this, number);
        setLastUpdateTime(System.currentTimeMillis());
    }

    /**
     * Increases the counter by `1`.
     */
    public void increaseCachePuts() {
        PUTS.incrementAndGet(this);
        setLastUpdateTime(System.currentTimeMillis());
    }

    /**
     * Increases the counter by the number specified.
     *
     * @param number the number by which the counter is increased.
     */
    public void increaseCachePuts(long number) {
        PUTS.addAndGet(this, number);
        setLastUpdateTime(System.currentTimeMillis());
    }

    /**
     * Increases the counter by `1`.
     */
    public void increaseCacheHits() {
        increaseCacheHits(1);
    }

    /**
     * Increases the counter by the number specified.
     *
     * @param number the number by which the counter is increased.
     */
    public void increaseCacheHits(long number) {
        hits.accumulate(number);
        setLastAccessTime(System.currentTimeMillis());
    }

    /**
     * Increases the counter by `1`.
     */
    public void increaseCacheMisses() {
        increaseCacheMisses(1);
    }

    /**
     * Increases the counter by the number specified.
     *
     * @param number the number by which the counter is increased.
     */
    public void increaseCacheMisses(long number) {
        misses.accumulate(number);
        setLastAccessTime(System.currentTimeMillis());
    }

    /**
     * Increases the counter by `1`.
     */
    public void increaseCacheEvictions() {
        EVICTIONS.incrementAndGet(this);
        setLastUpdateTime(System.currentTimeMillis());
    }

    /**
     * Increases the counter by the number specified.
     *
     * @param number the number by which the counter is increased.
     */
    public void increaseCacheEvictions(long number) {
        EVICTIONS.addAndGet(this, number);
        setLastUpdateTime(System.currentTimeMillis());
    }

    /**
     * Increments the getCache time accumulator.
     *
     * @param duration the time taken in nanoseconds.
     */
    public void addGetTimeNanos(long duration) {
        for (; ; ) {
            long nanos = getCacheTimeTakenNanos;
            if (nanos <= Long.MAX_VALUE - duration) {
                if (GET_CACHE_TIME_TAKEN_NANOS.compareAndSet(this, nanos, nanos + duration)) {
                    return;
                }
            } else {
                //counter full. Just reset.
                if (GET_CACHE_TIME_TAKEN_NANOS.compareAndSet(this, nanos, duration)) {
                    clear();
                    return;
                }
            }
        }
    }

    /**
     * Increments the put time accumulator.
     *
     * @param duration the time taken in nanoseconds.
     */
    public void addPutTimeNanos(long duration) {
        for (; ; ) {
            long nanos = putTimeTakenNanos;
            if (nanos <= Long.MAX_VALUE - duration) {
                if (PUT_TIME_TAKEN_NANOS.compareAndSet(this, nanos, nanos + duration)) {
                    return;
                }
            } else {
                //counter full. Just reset.
                if (PUT_TIME_TAKEN_NANOS.compareAndSet(this, nanos, duration)) {
                    clear();
                    return;
                }
            }
        }
    }

    /**
     * Increments the remove time accumulator.
     *
     * @param duration the time taken in nanoseconds.
     */
    public void addRemoveTimeNanos(long duration) {
        for (; ; ) {
            long nanos = removeTimeTakenNanos;
            if (nanos <= Long.MAX_VALUE - duration) {
                if (REMOVE_TIME_TAKEN_NANOS.compareAndSet(this, nanos, nanos + duration)) {
                    return;
                }
            } else {
                //counter full. Just reset.
                if (REMOVE_TIME_TAKEN_NANOS.compareAndSet(this, nanos, duration)) {
                    clear();
                    return;
                }
            }
        }
    }

    @Override
    public NearCacheStats getNearCacheStatistics() {
        throw new UnsupportedOperationException("Near Cache is not supported at server");
    }

    @Override
    public String toString() {
        return "CacheStatisticsImpl{"
                + "creationTime=" + creationTime
                + ", lastAccessTime=" + lastAccessTime
                + ", lastUpdateTime=" + lastUpdateTime
                + ", ownedEntryCount=" + getOwnedEntryCount()
                + ", removals=" + removals
                + ", expiries=" + expiries
                + ", puts=" + puts
                + ", hits=" + hits
                + ", misses=" + misses
                + ", evictions=" + evictions
                + ", putTimeTakenNanos=" + putTimeTakenNanos
                + ", getCacheTimeTakenNanos=" + getCacheTimeTakenNanos
                + ", removeTimeTakenNanos=" + removeTimeTakenNanos
                + ", replicationStats=" + replicationStats
                + '}';
    }
}
