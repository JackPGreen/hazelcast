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

import com.hazelcast.config.CacheDeserializedValues;
import com.hazelcast.config.IndexConfig;
import com.hazelcast.internal.serialization.SerializationService;
import com.hazelcast.map.impl.MapDataSerializerHook;
import com.hazelcast.map.impl.MapService;
import com.hazelcast.map.impl.record.Records;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.query.impl.CachedQueryEntry;
import com.hazelcast.query.impl.Index;
import com.hazelcast.query.impl.IndexRegistry;
import com.hazelcast.query.impl.IndexUtils;
import com.hazelcast.query.impl.InternalIndex;
import com.hazelcast.query.impl.QueryableEntry;
import com.hazelcast.spi.impl.AllowedDuringPassiveState;
import com.hazelcast.spi.impl.operationservice.BackupAwareOperation;
import com.hazelcast.spi.impl.operationservice.MutatingOperation;
import com.hazelcast.spi.impl.operationservice.Operation;
import com.hazelcast.spi.impl.operationservice.PartitionAwareOperation;

import java.io.IOException;

import static com.hazelcast.config.CacheDeserializedValues.NEVER;

public class AddIndexOperation extends MapOperation
        implements PartitionAwareOperation, MutatingOperation, BackupAwareOperation,
                    // AddIndexOperation is used when map proxy for IMap with indexes is initialized during passive state
                    // (e.g. IMap is read for the first time after HotRestart recovery when the cluster is still in PASSIVE state)
                    AllowedDuringPassiveState {
    /**
     * Configuration of the index.
     */
    private IndexConfig config;

    public AddIndexOperation() {
        // No-op.
    }

    /**
     * @param name Index name
     * @param config Index configuration. Must be normalized before passing to this operation.
     */
    AddIndexOperation(String name, IndexConfig config) {
        super(name);

        assert IndexUtils.validateAndNormalize(name, config).equals(config) : "Index configuration must be normalized";
        this.config = config;
    }

    @Override
    public boolean shouldBackup() {
        return mapContainer.getTotalBackupCount() > 0;
    }

    @Override
    public int getSyncBackupCount() {
        return mapContainer.getTotalBackupCount();
    }

    @Override
    public int getAsyncBackupCount() {
        return 0;
    }

    @Override
    public Operation getBackupOperation() {
        return new AddIndexBackupOperation(name, config);
    }

    @Override
    public String getServiceName() {
        return MapService.SERVICE_NAME;
    }

    @Override
    public void runInternal() {
        int partitionId = getPartitionId();

        IndexRegistry indexRegistry = mapContainer.getOrCreateIndexRegistry(partitionId);
        InternalIndex index = indexRegistry.addOrGetIndex(config);
        if (index.hasPartitionIndexed(partitionId)) {
            return;
        }

        SerializationService serializationService = getNodeEngine().getSerializationService();

        index.beginPartitionUpdate();

        CacheDeserializedValues cacheDeserializedValues = mapContainer.getMapConfig().getCacheDeserializedValues();
        CachedQueryEntry<?, ?> cachedEntry = cacheDeserializedValues == NEVER ? new CachedQueryEntry<>(serializationService,
                mapContainer.getExtractors()) : null;
        recordStore.forEach((dataKey, record) -> {
            Object value = Records.getValueOrCachedValue(record, serializationService);
            QueryableEntry<?, ?> queryEntry = mapContainer.newQueryEntry(dataKey, value);
            queryEntry.setRecord(record);
            CachedQueryEntry<?, ?> newEntry =
                    cachedEntry == null ? (CachedQueryEntry<?, ?>) queryEntry : cachedEntry.init(dataKey, value);
            index.putEntry(newEntry, null, queryEntry, Index.OperationSource.USER);
        }, false, false);

        index.markPartitionAsIndexed(partitionId);

        // Register index after we are done creating it and before sending response to the user.
        // It would be better to do once on each member instead of for each partition
        // but currently there is no appropriate operation for that as index must be registered on each member.
        mapServiceContext.registerIndex(name, config);
    }

    @Override
    public Object getResponse() {
        return Boolean.TRUE;
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        super.writeInternal(out);
        out.writeObject(config);
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        super.readInternal(in);
        config = in.readObject();
    }

    @Override
    public int getClassId() {
        return MapDataSerializerHook.ADD_INDEX;
    }

}
