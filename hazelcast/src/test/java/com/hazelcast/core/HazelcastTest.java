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

package com.hazelcast.core;

import com.hazelcast.config.Config;
import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.hazelcast.core.Hazelcast.getAllHazelcastInstances;
import static java.util.Collections.emptySet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class HazelcastTest extends HazelcastTestSupport {

    public static final String HAZELCAST_CONFIG = "hazelcast.config";

    @Before
    @After
    public void cleanup() {
        Hazelcast.shutdownAll();
        assertEquals(emptySet(), getAllHazelcastInstances());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOrCreateHazelcastInstance_nullConfig() {
        Hazelcast.getOrCreateHazelcastInstance(null);
    }

    @Test
    public void getOrCreateDefaultHazelcastInstance() {
        String hzConfigProperty = System.getProperty(HAZELCAST_CONFIG);
        try {
            System.setProperty(HAZELCAST_CONFIG, "classpath:test-hazelcast-jcache.xml");
            HazelcastInstance hz1 = Hazelcast.getOrCreateHazelcastInstance();
            HazelcastInstance hz2 = Hazelcast.getOrCreateHazelcastInstance();
            assertEquals("Calling two times getOrCreateHazelcastInstance should return same instance", hz1,
                    hz2);
        } finally {
            if (hzConfigProperty == null) {
                System.clearProperty(HAZELCAST_CONFIG);
            } else {
                System.setProperty(HAZELCAST_CONFIG, hzConfigProperty);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOrCreateHazelcastInstance_nullName() {
        Config config = new Config();
        Hazelcast.getOrCreateHazelcastInstance(config);
    }

    @Test
    public void getOrCreateHazelcastInstance_noneExisting() {
        Config config = new Config(randomString());
        config.setClusterName(randomString());

        HazelcastInstance hz = Hazelcast.getOrCreateHazelcastInstance(config);

        assertNotNull(hz);
        assertEquals(config.getInstanceName(), hz.getName());
        assertSame(hz, Hazelcast.getHazelcastInstanceByName(config.getInstanceName()));
        hz.shutdown();
    }

    @Test
    public void getOrCreateHazelcastInstance_existing() {
        Config config = new Config(randomString());
        config.setClusterName(randomString());

        HazelcastInstance hz1 = Hazelcast.newHazelcastInstance(config);

        HazelcastInstance hz2 = Hazelcast.getOrCreateHazelcastInstance(config);

        assertSame(hz1, hz2);
        hz1.shutdown();
    }

    @Test
    public void testNewInstanceByName() {
        Config config = new Config();
        config.setInstanceName("test");

        HazelcastInstance hc1 = Hazelcast.newHazelcastInstance(config);
        HazelcastInstance hc2 = Hazelcast.getHazelcastInstanceByName("test");
        HazelcastInstance hc3 = Hazelcast.getHazelcastInstanceByName(hc1.getName());

        assertSame(hc1, hc2);
        assertSame(hc1, hc3);
    }

    @Test(expected = InvalidConfigurationException.class)
    public void testNewInstanceByNameFail() {
        Config config = new Config();
        config.setInstanceName("test");
        Hazelcast.newHazelcastInstance(config);
        Hazelcast.newHazelcastInstance(config);
    }

}
