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

package com.hazelcast.map.impl.operation;

import com.hazelcast.cluster.Address;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.core.EntryEventType;
import com.hazelcast.core.ReadOnly;
import com.hazelcast.internal.monitor.impl.LocalMapStatsImpl;
import com.hazelcast.internal.partition.IPartitionService;
import com.hazelcast.internal.serialization.Data;
import com.hazelcast.internal.serialization.InternalSerializationService;
import com.hazelcast.internal.util.Clock;
import com.hazelcast.internal.util.Timer;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.ExtendedMapEntry;
import com.hazelcast.map.impl.LazyMapEntry;
import com.hazelcast.map.impl.LocalMapStatsProvider;
import com.hazelcast.map.impl.LockAwareLazyMapEntry;
import com.hazelcast.map.impl.MapContainer;
import com.hazelcast.map.impl.MapServiceContext;
import com.hazelcast.map.impl.event.MapEventPublisher;
import com.hazelcast.map.impl.record.Record;
import com.hazelcast.map.impl.recordstore.RecordStore;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import com.hazelcast.query.impl.QueryableEntry;
import com.hazelcast.spi.impl.NodeEngine;
import com.hazelcast.spi.impl.eventservice.EventService;
import com.hazelcast.spi.impl.operationservice.BackupOperation;

import java.util.Map.Entry;

import static com.hazelcast.config.InMemoryFormat.OBJECT;
import static com.hazelcast.core.EntryEventType.ADDED;
import static com.hazelcast.core.EntryEventType.REMOVED;
import static com.hazelcast.core.EntryEventType.UPDATED;
import static com.hazelcast.internal.util.ToHeapDataConverter.toHeapData;
import static com.hazelcast.map.impl.MapService.SERVICE_NAME;
import static com.hazelcast.map.impl.record.Record.UNSET;
import static com.hazelcast.wan.impl.CallerProvenance.NOT_WAN;

/**
 * Operator for single key processing logic of {@link
 * EntryProcessor} and backup entry processor related operations.
 */

@SuppressWarnings("checkstyle:methodcount")
public final class EntryOperator {
    private final boolean shouldClone;
    private final boolean backup;
    private final boolean readOnly;
    private final boolean wanReplicationEnabled;
    /** Are any listeners (e.g. {@link EntryUpdatedListener} registered to events */
    private final boolean hasEventRegistration;
    private final int partitionId;
    private final long startTimeNanos = Timer.nanos();
    private final String mapName;
    private final RecordStore recordStore;
    private final InternalSerializationService ss;
    private final MapContainer mapContainer;
    private final MapEventPublisher mapEventPublisher;
    private final LocalMapStatsImpl stats;
    private final IPartitionService partitionService;
    private final Predicate predicate;
    private final MapServiceContext mapServiceContext;
    private final MapOperation mapOperation;
    private final Address callerAddress;
    private final InMemoryFormat inMemoryFormat;

    private EntryProcessor entryProcessor;
    private EntryProcessor backupProcessor;

    // these fields can be used to reset this operator
    private boolean didMatchPredicate;
    private Data dataKey;
    private Object oldValue;
    /**
     * A clone of {@link #oldValue} that can be passed to the {@link #mapEventPublisher} to ensure isolation from any subsequent
     * mutations (e.g. in an {@link EntryProcessor})
     *
     * @see <a href="https://hazelcast.atlassian.net/browse/HZ-2837">HZ-2837 - Field level mutation being taken by listener as
     *      old value but not being considered by interceptor - Strange Behaviour</a>
     */
    private Object oldValueClone;
    private EntryEventType eventType;
    private Data result;
    private LockAwareLazyMapEntry entry;

    @SuppressWarnings("checkstyle:executablestatementcount")
    private EntryOperator(MapOperation mapOperation, Object processor, Predicate predicate, boolean collectWanEvents) {
        this.backup = mapOperation instanceof BackupOperation;
        setProcessor(processor);
        this.mapOperation = mapOperation;
        this.predicate = predicate;
        this.recordStore = mapOperation.recordStore;
        this.readOnly = entryProcessor instanceof ReadOnly;
        this.mapContainer = recordStore.getMapContainer();
        this.inMemoryFormat = mapContainer.getMapConfig().getInMemoryFormat();
        this.mapName = mapContainer.getName();
        this.wanReplicationEnabled = mapContainer.getWanContext().isWanReplicationEnabled();
        this.shouldClone = mapContainer.shouldCloneOnEntryProcessing(mapOperation.getPartitionId());
        this.mapServiceContext = mapContainer.getMapServiceContext();
        LocalMapStatsProvider localMapStatsProvider = mapServiceContext.getLocalMapStatsProvider();
        this.stats = localMapStatsProvider.getLocalMapStatsImpl(mapName);
        NodeEngine nodeEngine = mapServiceContext.getNodeEngine();
        this.ss = ((InternalSerializationService) nodeEngine.getSerializationService());
        this.partitionService = nodeEngine.getPartitionService();
        EventService eventService = nodeEngine.getEventService();
        this.hasEventRegistration = eventService.hasEventRegistration(SERVICE_NAME, mapName);
        this.mapEventPublisher = mapServiceContext.getMapEventPublisher();
        this.partitionId = recordStore.getPartitionId();
        this.callerAddress = mapOperation.getCallerAddress();
        this.entry = new LockAwareLazyMapEntry();
    }

    public EntryProcessor getEntryProcessor() {
        return entryProcessor;
    }

    private void setProcessor(Object processor) {
        if (backup) {
            backupProcessor = ((EntryProcessor) processor);
            entryProcessor = null;
        } else {
            entryProcessor = ((EntryProcessor) processor);
            backupProcessor = null;
        }
    }

    public static EntryOperator operator(MapOperation mapOperation) {
        return new EntryOperator(mapOperation, null, null, false);
    }

    public static EntryOperator operator(MapOperation mapOperation, Object processor) {
        return new EntryOperator(mapOperation, processor, null, false);
    }

    public static EntryOperator operator(MapOperation mapOperation, Object processor, Predicate predicate) {
        return new EntryOperator(mapOperation, processor, predicate, false);
    }

    public EntryOperator init(Data dataKey, Object oldValue, Object newValue, Data result,
                              EntryEventType eventType, Boolean locked, boolean changeExpiryOnUpdate, long ttl) {
        this.dataKey = dataKey;
        this.oldValue = oldValue;
        this.oldValueClone = oldValue;
        this.eventType = eventType;
        this.result = result;
        this.didMatchPredicate = true;
        this.entry.init(ss, dataKey, newValue != null ? newValue : oldValue,
                mapContainer.getExtractors(), locked, ttl, changeExpiryOnUpdate);
        return this;
    }

    public boolean isDidMatchPredicate() {
        return didMatchPredicate;
    }

    public LockAwareLazyMapEntry getEntry() {
        return entry;
    }

    public EntryOperator operateOnKey(Data dataKey) {
        init(dataKey, null, null, null, null,
                null, true, UNSET);

        if (belongsAnotherPartition(dataKey)) {
            return this;
        }

        oldValue = recordStore.get(dataKey, backup, callerAddress, false);

        // predicated entry processors can only be applied to existing entries
        // so if we have a predicate and somehow(due to expiration or split-brain healing)
        // we found value null, we should skip that entry.
        if (predicate != null && oldValue == null) {
            return this;
        }

        Boolean locked = recordStore.isLocked(dataKey);
        init(dataKey, clonedOrRawOldValue(), null, null, null,
                locked, true, UNSET);
        return operateOnKeyValueInternal();
    }

    public boolean checkCanProceed() {
        if (belongsAnotherPartition(dataKey)) {
            return false;
        }

        // predicated entry processors can only be applied to existing entries
        // so if we have a predicate and somehow(due to expiration or split-brain healing)
        // we found value null, we should skip that entry.
        if (predicate != null && oldValue == null) {
            return false;
        }

        return true;
    }

    public EntryOperator operateOnKeyValue(Data dataKey, Object oldValue) {
        init(dataKey, oldValue, null, null, null, null, true, UNSET);
        return operateOnKeyValueInternal();
    }

    public EntryOperator operateOnKeyValueInternal() {
        if (outOfPredicateScope(entry)) {
            this.didMatchPredicate = false;
            return this;
        }

        // Not required for OBJECT as #getOrNullOldValue() would
        // return null in any case where mutation would be problematic
        if (!readOnly && hasEventRegistration && inMemoryFormat != OBJECT) {
            oldValueClone = mapServiceContext.toData(oldValue);
        } else {
            oldValueClone = oldValue;
        }

        process(entry);
        findModificationType(entry);

        if (readOnly && entryWasModified()) {
            throwModificationInReadOnlyException();
        }

        return this;
    }

    private boolean entryWasModified() {
        return eventType != null;
    }

    public EntryEventType getEventType() {
        return eventType;
    }

    public Object getByPreferringDataNewValue() {
        return entry.getByPrioritizingDataValue();
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getOldValueClone() {
        return oldValueClone;
    }

    public Data getResult() {
        return result;
    }

    public EntryOperator doPostOperateOps() {
        if (!didMatchPredicate) {
            return this;
        }
        if (eventType == null) {
            // when event type is null, it means this is a
            // read-only entry processor and not modified entry.
            onTouched();
            return this;
        }
        switch (eventType) {
            case UPDATED:
                onTouched();
                onAddedOrUpdated();
                break;
            case ADDED:
                onAddedOrUpdated();
                break;
            case REMOVED:
                onRemove();
                break;
            default:
                throw new IllegalArgumentException("Unexpected event found:" + eventType);
        }

        doPostOperateOps0();
        return this;
    }

    public void doPostOperateOps0() {
        if (wanReplicationEnabled) {
            publishWanReplicationEvent();
        }

        if (!backup) {
            if (hasEventRegistration) {
                publishEntryEvent();
            }
            mapOperation.invalidateNearCache(dataKey);
        }

        mapOperation.evict(dataKey);
    }

    private void onAddedOrUpdated() {
        Object newValue = extractNewValue();
        if (backup) {
            recordStore.putBackupForEntryProcessor(dataKey, newValue, entry.isChangeExpiryOnUpdate(),
                    entry.getNewTtl(), UNSET, UNSET, NOT_WAN);
        } else {
            recordStore.setWithUncountedAccess(dataKey, newValue, entry.isChangeExpiryOnUpdate(),
                    entry.getNewTtl(), UNSET);
            onAddedOrUpdated0(newValue);
        }
    }

    public void onAddedOrUpdated0(Object newValue) {
        if (mapOperation.isPostProcessingOrHasInterceptor(recordStore)) {
            Record record = recordStore.getRecord(dataKey);
            newValue = record == null ? null : record.getValue();
            entry.setValueByInMemoryFormat(inMemoryFormat, newValue);
        }
        mapServiceContext.interceptAfterPut(mapContainer.getInterceptorRegistry(), newValue);
        stats.incrementPutLatencyNanos(Timer.nanosElapsed(startTimeNanos));
    }

    public Object extractNewValue() {
        return inMemoryFormat == OBJECT
                ? entry.getValue() : entry.getByPrioritizingDataValue();
    }

    private void onRemove() {
        if (backup) {
            recordStore.removeBackup(dataKey, NOT_WAN);
        } else {
            recordStore.delete(dataKey, NOT_WAN);
            onRemove0();
        }
    }

    public void onRemove0() {
        mapServiceContext.interceptAfterRemove(mapContainer.getInterceptorRegistry(), oldValue);
        stats.incrementRemoveLatencyNanos(Timer.nanosElapsed(startTimeNanos));
    }

    public Object clonedOrRawOldValue() {
        return shouldClone ? ss.toObject(ss.toData(oldValue)) : oldValue;
    }

    // Needed for MultipleEntryOperation.
    public boolean belongsAnotherPartition(Data key) {
        return partitionService.getPartitionId(key) != partitionId;
    }

    private boolean outOfPredicateScope(Entry entry) {
        assert entry instanceof QueryableEntry;

        if (predicate == null || predicate == Predicates.alwaysTrue()) {
            return false;
        }

        return predicate == Predicates.alwaysFalse() || !predicate.apply(entry);
    }

    public void findModificationType(LazyMapEntry mapEntry) {
        if (!mapEntry.isModified()
                || (oldValue == null && mapEntry.hasNullValue())) {
            // read only
            eventType = null;
            return;
        }

        if (mapEntry.hasNullValue()) {
            eventType = REMOVED;
            return;
        }

        eventType = oldValue == null ? ADDED : UPDATED;
    }

    public void onTouched() {
        // updates access time if record exists
        Record record = recordStore.getRecord(dataKey);
        if (record != null) {
            recordStore.accessRecord(dataKey, record, Clock.currentTimeMillis());
        }
    }

    public void process(ExtendedMapEntry entry) {
        if (backup) {
            backupProcessor.process(entry);
            return;
        }

        result = ss.toData(entryProcessor.process(entry));
    }

    private void throwModificationInReadOnlyException() {
        throw new UnsupportedOperationException("Entry Processor " + entryProcessor.getClass().getName()
                + " marked as ReadOnly tried to modify map " + mapName + ". This is not supported. Remove "
                + "the ReadOnly marker from the Entry Processor or do not modify the entry in the process "
                + "method.");
    }

    private void publishWanReplicationEvent() {
        assert entryWasModified();

        if (eventType == REMOVED) {
            mapOperation.publishWanRemove(dataKey);
        } else {
            mapOperation.publishWanUpdate(dataKey, entry.getByPrioritizingDataValue());
        }
    }

    private void publishEntryEvent() {
        Object oldValue = getOrNullOldValue();
        mapEventPublisher.publishEvent(callerAddress, mapName, eventType,
                toHeapData(dataKey), oldValue, entry.getByPrioritizingDataValue());
    }

    /**
     * @return {@code null} {@code oldValue} if in memory format is {@link InMemoryFormat#OBJECT} and operation is not
     *         {@link EntryEventType#REMOVED}, since old and new value in fired {@link EntryEvent} may reference the same object
     *         due to {@link InMemoryFormat#OBJECT}.
     */
    private Object getOrNullOldValue() {
        if (inMemoryFormat == OBJECT && eventType != REMOVED) {
            return null;
        } else {
            return oldValueClone;
        }
    }
}
