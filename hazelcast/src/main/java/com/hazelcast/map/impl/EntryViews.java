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

package com.hazelcast.map.impl;

import com.hazelcast.core.EntryView;
import com.hazelcast.internal.serialization.Data;
import com.hazelcast.internal.serialization.SerializationService;
import com.hazelcast.map.impl.record.Record;
import com.hazelcast.map.impl.recordstore.expiry.ExpiryMetadata;
import com.hazelcast.map.impl.wan.WanMapEntryView;

/**
 * A class providing static factory methods that create various entry view objects.
 */
public final class EntryViews {

    private EntryViews() {
    }

    public static <K, V> EntryView<K, V> createSimpleEntryView() {
        return new SimpleEntryView<>();
    }

    public static <K, V> EntryView<K, V> createSimpleEntryView(K key, V value, Record<V> record,
                                                               ExpiryMetadata expiryMetadata) {
        return new SimpleEntryView<>(key, value)
                .withCost(record.getCost())
                .withVersion(record.getVersion())
                .withHits(record.getHits())
                .withLastAccessTime(record.getLastAccessTime())
                .withLastUpdateTime(calculateLastUpdateTime(record, expiryMetadata))
                .withCreationTime(record.getCreationTime())
                .withLastStoredTime(record.getLastStoredTime())
                .withTtl(expiryMetadata.getTtl())
                .withMaxIdle(expiryMetadata.getMaxIdle())
                .withExpirationTime(expiryMetadata.getExpirationTime());
    }

    public static <K, V> WanMapEntryView<K, V> createWanEntryView(Data key, Data value,
                                                                  Record<V> record, ExpiryMetadata expiryMetadata,
                                                                  SerializationService serializationService) {
        return new WanMapEntryView<K, V>(key, value, serializationService)
                .withCost(record.getCost())
                .withVersion(record.getVersion())
                .withHits(record.getHits())
                .withLastAccessTime(record.getLastAccessTime())
                .withLastUpdateTime(calculateLastUpdateTime(record, expiryMetadata))
                .withCreationTime(record.getCreationTime())
                .withLastStoredTime(record.getLastStoredTime())
                .withTtl(expiryMetadata.getTtl())
                .withMaxIdle(expiryMetadata.getMaxIdle())
                .withExpirationTime(expiryMetadata.getExpirationTime());
    }

    private static <V> long calculateLastUpdateTime(Record<V> record, ExpiryMetadata expiryMetadata) {
        if (record.getLastUpdateTime() != Record.UNSET) {
            return record.getLastUpdateTime();
        }

        if (expiryMetadata != ExpiryMetadata.NULL) {
            return expiryMetadata.getLastUpdateTime();
        }

        return Record.UNSET;
    }
}
