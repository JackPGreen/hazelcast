/*
 * Copyright 2025 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.sql.impl.inject;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.sql.impl.expression.ExpressionEvalContext;

public final class JsonUpsertTargetDescriptor implements UpsertTargetDescriptor {

    public static final JsonUpsertTargetDescriptor INSTANCE = new JsonUpsertTargetDescriptor();

    private JsonUpsertTargetDescriptor() {
    }

    @Override
    public UpsertTarget create(ExpressionEvalContext evalContext) {
        return new JsonUpsertTarget();
    }

    @Override
    public void writeData(ObjectDataOutput out) {
    }

    @Override
    public void readData(ObjectDataInput in) {
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof JsonUpsertTargetDescriptor;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
