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

package com.hazelcast.cache.impl.operation;

import com.hazelcast.cache.impl.CacheDataSerializerHook;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.spi.impl.operationservice.Operation;
import com.hazelcast.spi.impl.operationservice.OperationFactory;

import java.io.IOException;

/**
 * Factory implementation of {@link CacheSizeOperation}.
 * @see CacheSizeOperation
 */
public class CacheSizeOperationFactory
        implements OperationFactory, IdentifiedDataSerializable {

    private String name;

    public CacheSizeOperationFactory() {
    }

    public CacheSizeOperationFactory(String name) {
        this.name = name;
    }

    @Override
    public Operation createOperation() {
        return new CacheSizeOperation(name);
    }

    @Override
    public int getFactoryId() {
        return CacheDataSerializerHook.F_ID;
    }

    @Override
    public int getClassId() {
        return CacheDataSerializerHook.SIZE_FACTORY;
    }

    @Override
    public void writeData(ObjectDataOutput out)
            throws IOException {
        out.writeString(name);
    }

    @Override
    public void readData(ObjectDataInput in)
            throws IOException {
        name = in.readString();
    }
}
