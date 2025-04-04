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

package com.hazelcast.map.impl.querycache.event;

import com.hazelcast.cluster.Address;
import com.hazelcast.internal.nio.IOUtil;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.internal.serialization.BinaryInterface;
import com.hazelcast.internal.serialization.Data;
import com.hazelcast.internal.serialization.SerializationService;
import com.hazelcast.internal.util.Clock;
import com.hazelcast.nio.serialization.impl.Versioned;

import java.io.IOException;
import java.util.Objects;

import static com.hazelcast.internal.cluster.Versions.V5_4;

/**
 * Default implementation of {@link QueryCacheEventData} which is sent to subscriber.
 */
@BinaryInterface
@SuppressWarnings({"CyclomaticComplexity", "DefaultQueryCacheEventData", "NPathComplexity"})
public class DefaultQueryCacheEventData implements QueryCacheEventData, Versioned {

    private Object key;
    private Object value;
    private Data dataKey;
    private Data dataNewValue;
    private Data dataOldValue;
    private long sequence;
    private SerializationService serializationService;
    private final long creationTime;
    private int eventType;
    private int partitionId;
    private String mapName;

    public DefaultQueryCacheEventData() {
        creationTime = Clock.currentTimeMillis();
    }

    public DefaultQueryCacheEventData(DefaultQueryCacheEventData other) {
        this.key = other.key;
        this.value = other.value;
        this.dataKey = other.dataKey;
        this.dataNewValue = other.dataNewValue;
        this.dataOldValue = other.dataOldValue;
        this.sequence = other.sequence;
        this.serializationService = other.serializationService;
        this.creationTime = other.creationTime;
        this.eventType = other.eventType;
        this.partitionId = other.partitionId;
        this.mapName = other.mapName;
    }

    @Override
    public Object getKey() {
        if (key == null && dataKey != null) {
            key = serializationService.toObject(dataKey);
        }
        return key;
    }

    @Override
    public Object getValue() {
        if (value == null && dataNewValue != null) {
            value = serializationService.toObject(dataNewValue);
        }
        return value;
    }

    @Override
    public Data getDataKey() {
        return dataKey;
    }

    @Override
    public Data getDataNewValue() {
        return dataNewValue;
    }

    @Override
    public Data getDataOldValue() {
        return dataOldValue;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public long getSequence() {
        return sequence;
    }

    @Override
    public int getPartitionId() {
        return partitionId;
    }

    @Override
    public int getEventType() {
        return eventType;
    }

    @Override
    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public void setDataKey(Data dataKey) {
        this.dataKey = dataKey;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setDataNewValue(Data dataNewValue) {
        this.dataNewValue = dataNewValue;
    }

    public void setDataOldValue(Data dataOldValue) {
        this.dataOldValue = dataOldValue;
    }

    public void setEventType(int eventType) {
        this.eventType = eventType;
    }

    public void setPartitionId(int partitionId) {
        this.partitionId = partitionId;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    @Override
    public void setSerializationService(SerializationService serializationService) {
        this.serializationService = serializationService;
    }

    @Override
    public String getSource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMapName() {
        return mapName;
    }

    @Override
    public Address getCaller() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeLong(sequence);
        IOUtil.writeData(out, dataKey);
        IOUtil.writeData(out, dataNewValue);
        out.writeInt(eventType);
        out.writeInt(partitionId);

        // RU_COMPAT_5_3
        if (out.getVersion().isGreaterOrEqual(V5_4)) {
            out.writeString(mapName);
        }
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        this.sequence = in.readLong();
        this.dataKey = IOUtil.readData(in);
        this.dataNewValue = IOUtil.readData(in);
        this.eventType = in.readInt();
        this.partitionId = in.readInt();

        // RU_COMPAT_5_3
        if (in.getVersion().isGreaterOrEqual(V5_4)) {
            this.mapName = in.readString();
        }
    }

    @Override
    public String toString() {
        return "DefaultQueryCacheEventData{"
                + "creationTime=" + creationTime
                + ", eventType=" + eventType
                + ", sequence=" + sequence
                + ", partitionId=" + partitionId
                + ", mapName=" + mapName
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultQueryCacheEventData that = (DefaultQueryCacheEventData) o;

        if (sequence != that.sequence) {
            return false;
        }
        if (eventType != that.eventType) {
            return false;
        }
        if (partitionId != that.partitionId) {
            return false;
        }
        if (!Objects.equals(key, that.key)) {
            return false;
        }
        if (!Objects.equals(value, that.value)) {
            return false;
        }
        if (!Objects.equals(dataKey, that.dataKey)) {
            return false;
        }
        if (!Objects.equals(dataNewValue, that.dataNewValue)) {
            return false;
        }
        if (!Objects.equals(dataOldValue, that.dataOldValue)) {
            return false;
        }
        if (!Objects.equals(mapName, that.mapName)) {
            return false;
        }
        return Objects.equals(serializationService, that.serializationService);
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (dataKey != null ? dataKey.hashCode() : 0);
        result = 31 * result + (dataNewValue != null ? dataNewValue.hashCode() : 0);
        result = 31 * result + (dataOldValue != null ? dataOldValue.hashCode() : 0);
        result = 31 * result + (int) (sequence ^ (sequence >>> 32));
        result = 31 * result + (serializationService != null ? serializationService.hashCode() : 0);
        result = 31 * result + eventType;
        result = 31 * result + partitionId;
        result = 31 * result + (mapName != null ? mapName.hashCode() : 0);
        return result;
    }
}
