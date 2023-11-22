/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.internal.namespace.replicatedmap;

import com.hazelcast.config.Config;
import com.hazelcast.config.EntryListenerConfig;
import com.hazelcast.map.IMap;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReplicatedMapEntryListenerUCDTest extends ReplicatedMapUCDTest {

    @Override
    public void test() throws Exception {
        map.put(1, 1, 1, TimeUnit.MILLISECONDS);
        IMap<String, Boolean> map = instance.getMap("ReplicatedMapEntryListenerUCDTest");

        //TODO: is there any need to test the other methods of the ReplicatedMapEntryListener ?
        assertTrueEventually(() -> {
            Boolean added = map.get("added");
            assertNotNull(added);
            assertTrue(added);
        });
    }

    @Override
    protected void mutateConfig(Config config) {
        EntryListenerConfig listenerConfig = new EntryListenerConfig();
        listenerConfig.setClassName(getUserDefinedClassNames()[0]);

        replicatedMapConfig.addEntryListenerConfig(listenerConfig);
        super.mutateConfig(config);
    }

    @Override
    protected String[] getUserDefinedClassNames() {
        return new String[] {"usercodedeployment.ReplicatedMapEntryListener"};
    }
}
