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

import com.hazelcast.internal.serialization.impl.SerializationUtil;
import com.hazelcast.map.impl.MapDataSerializerHook;
import com.hazelcast.map.impl.MapService;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.spi.impl.operationservice.Operation;
import com.hazelcast.spi.impl.operationservice.impl.operations.PartitionAwareOperationFactory;
import com.hazelcast.spi.merge.SplitBrainMergePolicy;
import com.hazelcast.spi.merge.SplitBrainMergeTypes.MapMergeTypes;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.hazelcast.internal.namespace.NamespaceUtil.callWithNamespace;

/**
 * Inserts the merging entries for all partitions of a member via locally invoked {@link MergeOperation}.
 *
 * @since 3.10
 */
@SuppressWarnings("WeakerAccess")
public class MergeOperationFactory extends PartitionAwareOperationFactory {

    protected String name;
    protected List<MapMergeTypes<Object, Object>>[] mergingEntries;
    protected SplitBrainMergePolicy<Object, MapMergeTypes<Object, Object>, Object> mergePolicy;

    public MergeOperationFactory() {
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public MergeOperationFactory(String name, int[] partitions, List<MapMergeTypes<Object, Object>>[] mergingEntries,
                                 SplitBrainMergePolicy<Object, MapMergeTypes<Object, Object>, Object> mergePolicy) {
        this.name = name;
        this.partitions = partitions;
        this.mergingEntries = mergingEntries;
        this.mergePolicy = mergePolicy;
    }

    @Override
    public Operation createPartitionOperation(int partitionId) {
        for (int i = 0; i < partitions.length; i++) {
            if (partitions[i] == partitionId) {
                return new MergeOperation(name, mergingEntries[i], mergePolicy, false);
            }
        }
        throw new IllegalArgumentException("Unknown partitionId " + partitionId + " (" + Arrays.toString(partitions) + ")");
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeString(name);
        out.writeIntArray(partitions);
        for (List<MapMergeTypes<Object, Object>> list : mergingEntries) {
            out.writeInt(list.size());
            for (MapMergeTypes<Object, Object> mergingEntry : list) {
                out.writeObject(mergingEntry);
            }
        }
        out.writeObject(mergePolicy);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        name = in.readString();
        partitions = in.readIntArray();
        //noinspection unchecked
        mergingEntries = new List[partitions.length];
        for (int partitionIndex = 0; partitionIndex < partitions.length; partitionIndex++) {
            mergingEntries[partitionIndex] = SerializationUtil.readList(in);
        }
        mergePolicy = callWithNamespace(in::readObject, name, MapService::lookupNamespace);
    }

    @Override
    public int getFactoryId() {
        return MapDataSerializerHook.F_ID;
    }

    @Override
    public int getClassId() {
        return MapDataSerializerHook.MERGE_FACTORY;
    }
}
