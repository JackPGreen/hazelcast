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
import com.hazelcast.map.impl.record.Record;
import com.hazelcast.map.impl.recordstore.StaticParams;
import com.hazelcast.map.impl.recordstore.expiry.ExpiryMetadata;
import com.hazelcast.spi.impl.operationservice.MutatingOperation;

public class PutTransientOperation extends BasePutOperation implements MutatingOperation {

    public PutTransientOperation() {
    }

    public PutTransientOperation(String name, Data dataKey, Data value) {
        super(name, dataKey, value);
    }

    @Override
    protected void runInternal() {
        oldValue = mapServiceContext.toData(recordStore.putTransient(dataKey,
                dataValue, getTtl(), getMaxIdle()));
    }

    @Override
    protected PutBackupOperation newBackupOperation(Data dataKey, Record record, Data dataValue) {
        ExpiryMetadata expiryMetadata = recordStore.getExpirySystem().getExpiryMetadata(dataKey);
        return new PutTransientBackupOperation(name, dataKey, record, dataValue, expiryMetadata);
    }

    @Override
    protected StaticParams getStaticParams() {
        return StaticParams.PUT_TRANSIENT_PARAMS;
    }

    @Override
    public void onWaitExpire() {
        sendResponse(null);
    }

    @Override
    public int getClassId() {
        return MapDataSerializerHook.PUT_TRANSIENT;
    }
}
