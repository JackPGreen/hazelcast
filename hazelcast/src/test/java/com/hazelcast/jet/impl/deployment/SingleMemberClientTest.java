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

package com.hazelcast.jet.impl.deployment;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.RoutingMode;
import com.hazelcast.cluster.Address;
import com.hazelcast.collection.IList;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.SimpleTestInClusterSupport;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.JobNotFoundException;
import com.hazelcast.jet.core.JobStatus;
import com.hazelcast.jet.impl.JetClientInstanceImpl;
import com.hazelcast.jet.impl.JetServiceBackend;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.test.Accessors;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import static com.hazelcast.jet.core.JobAssertions.assertThat;
import static com.hazelcast.jet.core.TestProcessors.streamingDag;
import static com.hazelcast.jet.impl.util.ExceptionUtil.isOrHasCause;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(HazelcastSerialClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class SingleMemberClientTest extends SimpleTestInClusterSupport {

    private static HazelcastInstance masterInstance;
    private static HazelcastInstance nonMasterInstance;
    private static HazelcastInstance masterClient;
    private static HazelcastInstance nonMasterClient;

    @BeforeClass
    public static void setUp() {
        initialize(2, null);
        masterInstance = instances()[0];
        nonMasterInstance = instances()[1];
        masterClient = createClientConnectingTo(masterInstance);
        nonMasterClient = createClientConnectingTo(nonMasterInstance);
    }

    private static HazelcastInstance createClientConnectingTo(HazelcastInstance targetInstance) {
        Address address = targetInstance.getCluster().getLocalMember().getAddress();
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().getClusterRoutingConfig().setRoutingMode(RoutingMode.SINGLE_MEMBER);
        clientConfig.getNetworkConfig().getAddresses().clear();
        clientConfig.getNetworkConfig().getAddresses().add(address.getHost() + ":" + address.getPort());
        return factory().newHazelcastClient(clientConfig);
    }

    @Test
    public void when_jobSubmitted_then_executedSuccessfully() {
        //Given
        String sourceName = "source";
        String sinkName = "sink";
        IList<Integer> list = masterInstance.getList(sourceName);
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }

        //When
        Pipeline p = Pipeline.create();
        p.readFrom(Sources.list(sourceName))
         .writeTo(Sinks.list(sinkName));
        nonMasterClient.getJet().newJob(p).join();

        //Then
        assertEquals(10, masterInstance.getList(sinkName).size());
    }

    @Test
    public void when_jobSubmitted_then_jobCanBeFetchedByIdOrName() {
        //Given
        String jobName = randomName();

        //When
        DAG dag = streamingDag();
        JetService jet = nonMasterClient.getJet();
        Job job = jet.newJob(dag, new JobConfig().setName(jobName));

        long jobId = job.getId();

        //Then
        assertTrueEventually(() -> {
            assertNotNull(jet.getJob(jobId));
            assertNotNull(jet.getJob(jobName));
            assertTrue(jet.getJobs().stream().anyMatch(j -> j.getId() == jobId));
            assertFalse(jet.getJobs(jobName).isEmpty());
            assertNotNull(jet.getJob(jobId).getStatus());
            assertEquals(JobStatus.RUNNING, jet.getJob(jobId).getStatus());
            Job j = jet.getJob(jobName);
            assertNotNull(j.getConfig());
            assertGreaterOrEquals("submissionTime", j.getSubmissionTime(), 0);
        }, 10);
    }


    @Test
    public void when_jobSuspended_then_jobStatusIsSuspended() {
        //Given
        Job job = startJobAndVerifyItIsRunning();

        //When
        job.suspend();

        //Then
        Job job1 = nonMasterClient.getJet().getJob(job.getName());
        assertThat(job1).eventuallyHasStatus(JobStatus.SUSPENDED);
    }

    @Test
    public void when_jobResumed_then_jobStatusIsRunning() {
        //Given
        Job job = startJobAndVerifyItIsRunning();
        job.suspend();
        String jobName = job.getName();
        Job job2 = nonMasterClient.getJet().getJob(jobName);
        assertThat(job2).eventuallyHasStatus(JobStatus.SUSPENDED);

        //When
        job.resume();

        //Then
        Job job1 = nonMasterClient.getJet().getJob(jobName);
        assertThat(job1).eventuallyHasStatus(JobStatus.RUNNING);
    }

    @Test
    public void when_jobCancelled_then_jobStatusIsCompleted() {
        //Given
        Job job = startJobAndVerifyItIsRunning();

        //When
        job.cancel();

        //Then
        Job job1 = nonMasterClient.getJet().getJob(job.getName());
        assertThat(job1).eventuallyHasStatus(JobStatus.FAILED);
    }

    @Test
    public void when_jobSummaryListIsAsked_then_jobSummaryListReturned() {
        // when
        Job job = startJobAndVerifyItIsRunning();

        // then
        JetClientInstanceImpl jet = (JetClientInstanceImpl) nonMasterClient.getJet();
        assertThat(jet.getJobSummaryList())
                .anyMatch(s -> s.getJobId() == job.getId());
        assertThat(jet.getJobAndSqlSummaryList())
                .anyMatch(s -> s.getJobId() == job.getId());
    }

    @Test
    public void when_lightJobSubmittedToNonMaster_then_coordinatedByNonMaster() {
        Job job = nonMasterClient.getJet().newLightJob(streamingDag());
        JetServiceBackend service = Accessors.getNodeEngineImpl(nonMasterInstance).getService(JetServiceBackend.SERVICE_NAME);
        assertTrueEventually(() ->
                assertNotNull(service.getJobCoordinationService().getLightMasterContexts().get(job.getId())));
    }

    @Test
    public void when_lightJobSubmittedToNonMaster_then_accessibleFromAllMembers() {
        Job job = nonMasterClient.getJet().newLightJob(streamingDag());
        assertTrueEventually(() -> {
            Job trackedJob = masterInstance.getJet().getJob(job.getId());
            assertNotNull(trackedJob);
            assertEquals(JobStatus.RUNNING, trackedJob.getStatus());
        });

        Job job1 = masterClient.getJet().getJob(job.getId());
        Job job2 = nonMasterClient.getJet().getJob(job.getId());
        assertNotNull(job1);
        assertNotNull(job2);
        assertNotEquals(0, job1.getSubmissionTime());
        assertEquals(job1.getSubmissionTime(), job2.getSubmissionTime());
        assertFalse(job1.getFuture().isDone());
        assertFalse(job2.getFuture().isDone());
        // Cancel requested through client connected to the master, but job is coordinated by the other member.
        // The jobX.getFuture() invokes JoinSubmittedJobOperation on a member asynchronously. The jobX.cancel()
        // invokes TerminateJobOperation. We have no control over which of these operation's doRun() method executes
        // first. If JoinSubmittedJobOperation is first, then join() throws CancellationException. If
        // TerminateJobOperation is first (that results later in removing of light master context) then the join() throws
        // CompletionException.
        job1.cancel();
        try {
            job1.join();
            fail("join didn't fail");
        } catch (CompletionException e) {
            assert isOrHasCause(e, JobNotFoundException.class);
        } catch (CancellationException ignored) { }
    }

    private Job startJobAndVerifyItIsRunning() {
        String jobName = randomName();
        DAG dag = streamingDag();
        Job job = nonMasterClient.getJet().newJob(dag, new JobConfig().setName(jobName));
        Job job1 = nonMasterClient.getJet().getJob(jobName);
        assertThat(job1).eventuallyHasStatus(JobStatus.RUNNING);
        return job;
    }
}
