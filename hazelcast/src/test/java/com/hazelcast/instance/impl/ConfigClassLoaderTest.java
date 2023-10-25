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

package com.hazelcast.instance.impl;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.Accessors;
import com.hazelcast.test.HazelcastTestSupport;
import org.junit.After;
import org.junit.Before;

/** Stub to get a qualified instance of {@link Node#getConfigClassLoader()} */
public abstract class ConfigClassLoaderTest extends HazelcastTestSupport {
    protected Config config;
    protected ClassLoader nodeClassLoader;

    protected HazelcastInstance lastInstance;

    public ConfigClassLoaderTest() {
        config = new Config();
        config.setClassLoader(HazelcastInstance.class.getClassLoader());
    }

    // TODO Better way of doing this? We need to create an instance (or mock lots of pieces that account for NS)
    //  and we need to reference that instance for NodeEngine context in tests
    protected void populateConfigClassLoader() {
        if (lastInstance != null) {
            lastInstance.getLifecycleService().terminate();
        }
        lastInstance = createHazelcastInstance(config);
        nodeClassLoader = Accessors.getNode(lastInstance).getConfigClassLoader();
    }
}
