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

package com.hazelcast.internal.serialization.impl;

import com.hazelcast.internal.serialization.impl.defaultserializers.ConstantSerializers;
import com.hazelcast.internal.serialization.impl.defaultserializers.JavaDefaultSerializers;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class SerializationTest extends HazelcastTestSupport {

    @Test
    public void testPrivateConstructors() {
        assertUtilityConstructor(FactoryIdHelper.class);
        assertUtilityConstructor(SerializationUtil.class);
        assertUtilityConstructor(JavaDefaultSerializers.class);
        assertUtilityConstructor(ConstantSerializers.class);
        assertUtilityConstructor(SerializationConstants.class);
    }
}
