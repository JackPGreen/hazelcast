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

package com.hazelcast.map.impl;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.MapStore;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

import static com.hazelcast.internal.nio.IOUtil.deleteQuietly;
import static com.hazelcast.test.Accessors.getNodeEngineImpl;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class StoreLatencyPlugin_MapIntegrationTest extends HazelcastTestSupport {

    private HazelcastInstance hz;
    private Map<Integer, String> map;

    @Before
    public void setup() throws Exception {
        Config config = new Config()
                .setProperty("hazelcast.diagnostics.enabled", "true")
                .setProperty("hazelcast.diagnostics.storeLatency.period.seconds", "1");

        MapConfig mapConfig = addMapConfig(config);

        hz = createHazelcastInstance(config);
        map = hz.getMap(mapConfig.getName());
    }

    @After
    public void after() {
        File file = getNodeEngineImpl(hz).getDiagnostics().currentFile();
        deleteQuietly(file);
    }

    @Test
    public void test() {
        for (int k = 0; k < 100; k++) {
            map.get(k);
        }

        assertTrueEventually(() -> {
            File file = getNodeEngineImpl(hz).getDiagnostics().currentFile();
            String content = Files.readString(file.toPath());
            assertContains(content, "mappy");
        });
    }

    private static MapConfig addMapConfig(Config config) {
        MapConfig mapConfig = config.getMapConfig("mappy");
        mapConfig.getMapStoreConfig()
                .setEnabled(true)
                .setImplementation(new MapStore<>() {
                    private final Random random = new Random();

                    @Override
                    public void store(Object key, Object value) {
                    }

                    @Override
                    public Object load(Object key) {
                        randomSleep();
                        return null;
                    }

                    private void randomSleep() {
                        long delay = random.nextInt(100);
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public Map<Object, Object> loadAll(Collection<Object> keys) {
                        return null;
                    }

                    @Override
                    public void storeAll(Map<Object, Object> map) {
                    }

                    @Override
                    public void delete(Object key) {
                    }

                    @Override
                    public Iterable<Object> loadAllKeys() {
                        return null;
                    }

                    @Override
                    public void deleteAll(Collection<Object> keys) {

                    }
                });
        return mapConfig;
    }
}
