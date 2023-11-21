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

package com.hazelcast.internal.namespace.iqueue;

import com.hazelcast.config.Config;
import com.hazelcast.config.ItemListenerConfig;
import com.hazelcast.map.IMap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class QueueItemListenerUCDTest extends IQueueUCDTest {
    @Override
    public void test() throws Exception {
        queue.add("item");
        queue.remove("item");
        IMap<String, Boolean> map = instance.getMap("QueueItemListenerUCDTest");
        assertNotNull(map);

        assertTrueEventually("The 'added' key should be set to true eventually", () -> {
            Boolean added = map.get("added");
            assertNotNull(added);
            assertTrue(added);
        });

        assertTrueEventually("The 'removed' key should be set to true eventually", () -> {
            Boolean removed = map.get("removed");
            assertNotNull(removed);
            assertTrue(removed);
        });
    }

    @Override
    protected void mutateConfig(Config config) {
        ItemListenerConfig listenerConfig = new ItemListenerConfig();
        listenerConfig.setClassName(getUserDefinedClassNames()[0]);

        queueConfig.addItemListenerConfig(listenerConfig);
        super.mutateConfig(config);
    }

    @Override
    protected String[] getUserDefinedClassNames() {
        return new String[]{"usercodedeployment.QueueItemListener"};
    }
}
