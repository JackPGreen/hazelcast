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

package com.hazelcast.config;

import com.hazelcast.internal.config.QueueConfigReadOnly;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Collections;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class QueueConfigReadOnlyTest {

    private QueueConfig getReadOnlyConfig() {
        return new QueueConfigReadOnly(new QueueConfig());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetName() {
        getReadOnlyConfig().setName("anyName");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetMaxSize() {
        getReadOnlyConfig().setMaxSize(23);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetBackupCount() {
        getReadOnlyConfig().setBackupCount(42);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetAsyncBackupCount() {
        getReadOnlyConfig().setAsyncBackupCount(23);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetEmptyQueueTtl() {
        getReadOnlyConfig().setEmptyQueueTtl(42);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetQueueStoreConfig() {
        getReadOnlyConfig().setQueueStoreConfig(new QueueStoreConfig());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetItemListenerConfigs() {
        getReadOnlyConfig().setItemListenerConfigs(Collections.emptyList());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddItemListenerConfig() {
        getReadOnlyConfig().addItemListenerConfig(new ItemListenerConfig());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetStatisticsEnabled() {
        getReadOnlyConfig().setStatisticsEnabled(true);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetSplitBrainProtectionName() {
        getReadOnlyConfig().setSplitBrainProtectionName("mySplitBrainProtection");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetMergePolicy() {
        getReadOnlyConfig().setMergePolicyConfig(new MergePolicyConfig());
    }
}
