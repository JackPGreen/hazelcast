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

package com.hazelcast.jet.core;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.core.TestProcessors.NoOutputSourceP;
import com.hazelcast.jet.impl.operation.SnapshotPhase1Operation;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.time.Duration;

import static com.hazelcast.jet.config.ProcessingGuarantee.EXACTLY_ONCE;
import static com.hazelcast.jet.config.ProcessingGuarantee.NONE;
import static com.hazelcast.jet.core.JobAssertions.assertThat;
import static com.hazelcast.jet.core.JobStatus.COMPLETING;
import static com.hazelcast.jet.core.JobStatus.RUNNING;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.junit.Assert.assertEquals;

@RunWith(HazelcastSerialClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class TerminalSnapshotSynchronizationTest extends JetTestSupport {

    private static final int NODE_COUNT = 2;

    @Before
    public void before() {
        TestProcessors.reset(1);
    }

    private Job setup(boolean snapshotting) {
        HazelcastInstance[] instances = createHazelcastInstances(NODE_COUNT);

        DAG dag = new DAG();
        dag.newVertex("generator", () -> new NoOutputSourceP()).localParallelism(1);

        JobConfig config = new JobConfig()
                .setProcessingGuarantee(snapshotting ? EXACTLY_ONCE : NONE)
                .setSnapshotIntervalMillis(DAYS.toMillis(1));
        Job job = instances[0].getJet().newJob(dag, config);
        assertThat(job).eventuallyHasStatus(RUNNING);
        return job;
    }

    @After
    public void after() {
        // to not affect other tests in this VM
        SnapshotPhase1Operation.postponeResponses = false;
    }

    @Test
    public void when_jobRestartedGracefully_then_waitsForSnapshot() {
        Job job = setup(true);

        // When
        SnapshotPhase1Operation.postponeResponses = true;
        job.restart();

        // Then
        var timeout = Duration.ofSeconds(5);
        assertThat(job).eventuallyHasStatus(COMPLETING, timeout);
        assertTrueAllTheTime(() -> assertEquals(COMPLETING, job.getStatus()), 5);
        SnapshotPhase1Operation.postponeResponses = false;
        assertThat(job).eventuallyHasStatus(RUNNING, timeout);
    }

    @Test
    public void when_jobRestartedForcefully_then_doesNotWaitForSnapshot() {
        Job job = setup(false);
        assertTrueEventually(() -> assertEquals(NODE_COUNT, NoOutputSourceP.initCount.get()), 10);

        // When
        SnapshotPhase1Operation.postponeResponses = true;
        job.restart();

        // Then
        assertTrueEventually(() -> assertEquals(4, NoOutputSourceP.initCount.get()), 5);
        assertThat(job).eventuallyHasStatus(RUNNING, Duration.ofSeconds(5));
    }
}
