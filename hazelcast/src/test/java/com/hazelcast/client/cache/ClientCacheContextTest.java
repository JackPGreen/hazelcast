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

package com.hazelcast.client.cache;

import com.hazelcast.cache.CacheContextTest;
import com.hazelcast.client.test.TestHazelcastFactory;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.hazelcast.cache.CacheTestSupport.createClientCachingProvider;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class ClientCacheContextTest extends CacheContextTest {

    private final TestHazelcastFactory factory = new TestHazelcastFactory();

    @Before
    @Override
    public void setup() {
        hazelcastInstance1 = factory.newHazelcastInstance();
        hazelcastInstance2 = factory.newHazelcastInstance();

        driverInstance = factory.newHazelcastClient();
        provider = createClientCachingProvider(driverInstance);
    }

    @After
    public void tearDown() {
        factory.shutdownAll();
        driverInstance.shutdown();
        hazelcastInstance1.shutdown();
        hazelcastInstance2.shutdown();
    }

    @Override
    @Test
    public void cacheEntryListenerCountIncreasedAfterRegisterAndDecreasedAfterDeregister() {
        cacheEntryListenerCountIncreasedAndDecreasedCorrectly(DecreaseType.DEREGISTER);
    }

    @Override
    @Test
    public void cacheEntryListenerCountIncreasedAfterRegisterAndDecreasedAfterShutdown() {
        cacheEntryListenerCountIncreasedAndDecreasedCorrectly(DecreaseType.SHUTDOWN);
    }

    @Override
    @Test
    public void cacheEntryListenerCountIncreasedAfterRegisterAndDecreasedAfterTerminate() {
        cacheEntryListenerCountIncreasedAndDecreasedCorrectly(DecreaseType.TERMINATE);
    }
}
