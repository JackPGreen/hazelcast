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

package com.hazelcast.internal.monitor.impl;

import com.hazelcast.internal.json.JsonObject;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class MemberPartitionStateImplTest {

    private MemberPartitionStateImpl memberPartitionState;

    @Before
    public void setUp() {
        memberPartitionState = new MemberPartitionStateImpl();

        memberPartitionState.setMemberStateSafe(true);
        memberPartitionState.getPartitions().add(5);
        memberPartitionState.getPartitions().add(18);
    }

    @Test
    public void testDefaultConstructor() {
        assertTrue(memberPartitionState.isMemberStateSafe());
        assertNotNull(memberPartitionState.getPartitions());
        assertEquals(2, memberPartitionState.getPartitions().size());
        assertNotNull(memberPartitionState.toString());
    }

    @Test
    public void testSerialization() {
        JsonObject serialized = memberPartitionState.toJson();
        MemberPartitionStateImpl deserialized = new MemberPartitionStateImpl();
        deserialized.fromJson(serialized);

        assertTrue(deserialized.isMemberStateSafe());
        assertNotNull(deserialized.getPartitions());
        assertEquals(2, deserialized.getPartitions().size());
        assertNotNull(deserialized.toString());
    }
}
