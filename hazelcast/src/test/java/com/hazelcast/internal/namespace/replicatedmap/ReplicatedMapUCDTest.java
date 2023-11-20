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
import com.hazelcast.config.MapConfig;
import com.hazelcast.internal.namespace.UCDTest;
import com.hazelcast.replicatedmap.ReplicatedMap;
import org.junit.Before;

import java.io.IOException;

public abstract class ReplicatedMapUCDTest extends UCDTest {
    protected MapConfig mapConfig;
    protected ReplicatedMap<Object, Object> map;

    @Override
    @Before
    public void setUp() throws IOException, ClassNotFoundException {
        mapConfig = new MapConfig(objectName);
        mapConfig.setNamespace(getNamespaceName());

        super.setUp();

        map = instance.getReplicatedMap(objectName);
    }

    @Override
    protected void mutateConfig(Config config) {
        config.addMapConfig(mapConfig);
    }
}
