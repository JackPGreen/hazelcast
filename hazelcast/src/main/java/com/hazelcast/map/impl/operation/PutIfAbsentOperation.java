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

import com.hazelcast.internal.serialization.Data;
import com.hazelcast.map.impl.MapDataSerializerHook;
import com.hazelcast.map.impl.operation.steps.engine.State;
import com.hazelcast.map.impl.recordstore.StaticParams;
import com.hazelcast.spi.impl.operationservice.MutatingOperation;

public class PutIfAbsentOperation
        extends BasePutOperation implements MutatingOperation {

    protected transient boolean successful;

    public PutIfAbsentOperation(String name, Data dataKey, Data value) {
        super(name, dataKey, value);
    }

    public PutIfAbsentOperation() {
    }

    @Override
    protected void runInternal() {
        Object oldValue = recordStore.putIfAbsent(dataKey, dataValue,
                getTtl(), getMaxIdle(), getCallerAddress());
        this.oldValue = mapServiceContext.toData(oldValue);
        this.successful = this.oldValue == null;
    }

    @Override
    protected StaticParams getStaticParams() {
        return StaticParams.PUT_IF_ABSENT_PARAMS;
    }

    @Override
    public void applyState(State state) {
        super.applyState(state);
        successful = oldValue == null;
    }

    @Override
    public void afterRunInternal() {
        if (successful) {
            super.afterRunInternal();
        }
    }

    @Override
    public Object getResponse() {
        return oldValue;
    }

    @Override
    public boolean shouldBackup() {
        return successful && super.shouldBackup();
    }

    @Override
    public int getClassId() {
        return MapDataSerializerHook.PUT_IF_ABSENT;
    }
}
