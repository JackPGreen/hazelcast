/*
 * Copyright 2025 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hazelcast.jet.kinesis;

import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.hazelcast.jet.JetException;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.JobStatus;
import com.hazelcast.jet.kinesis.impl.AwsConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.test.TestSources;
import com.hazelcast.jet.test.SerialTest;
import com.hazelcast.test.annotation.NightlyTest;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import static com.hazelcast.internal.util.ExceptionUtil.sneakyThrow;
import static com.hazelcast.jet.Util.entry;
import static com.hazelcast.jet.core.JobAssertions.assertThat;
import static com.hazelcast.jet.kinesis.KinesisSinks.MAXIMUM_KEY_LENGTH;
import static com.hazelcast.jet.kinesis.KinesisSinks.MAX_RECORD_SIZE;
import static com.hazelcast.test.DockerTestUtil.assumeDockerEnabled;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.testcontainers.utility.DockerImageName.parse;

@SuppressWarnings("StaticVariableName")
@Category(NightlyTest.class)
public class KinesisFailureTest extends AbstractKinesisTest {

    @ClassRule
    public static final Network NETWORK = Network.newNetwork();
    private static LocalStackContainer localStack;

    private static ToxiproxyContainer toxiproxy;
    private static AwsConfig AWS_CONFIG;
    private static AmazonKinesisAsync KINESIS;
    private static Proxy PROXY;
    private static KinesisTestHelper HELPER;

    private static final String NETWORK_ALIAS = "toxiproxy";
    private static final String UPSTREAM = "toxiproxy:4566";

    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisFailureTest.class);

    public KinesisFailureTest() {
        super(AWS_CONFIG, KINESIS, HELPER);
    }

    @BeforeClass
    public static void beforeClass() throws IOException {
        assumeDockerEnabled();

        localStack = new LocalStackContainer(parse("localstack/localstack")
                .withTag(LOCALSTACK_VERSION))
                .withNetwork(NETWORK)
                .withNetworkAliases(NETWORK_ALIAS)
                .withServices(Service.KINESIS)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER));
        localStack.start();
        toxiproxy = new ToxiproxyContainer(parse("ghcr.io/shopify/toxiproxy")
                .withTag("2.5.0"))
                .withNetwork(NETWORK)
                .withNetworkAliases(NETWORK_ALIAS)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER));
        toxiproxy.start();

        PROXY = initProxy(toxiproxy);

        String host = toxiproxy.getHost();
        Integer port = toxiproxy.getMappedPort(8666);

        AWS_CONFIG = new AwsConfig()
                .withEndpoint("http://" + host + ":" + port)
                .withRegion(localStack.getRegion())
                .withCredentials(localStack.getAccessKey(), localStack.getSecretKey());
        KINESIS = AWS_CONFIG.buildClient();
        HELPER = new KinesisTestHelper(KINESIS, STREAM);
    }

    private static Proxy initProxy(ToxiproxyContainer toxiproxy) throws IOException {
        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
        return toxiproxyClient.createProxy(NETWORK_ALIAS, "0.0.0.0:8666", UPSTREAM);
    }
    @AfterClass
    public static void afterClass() {
        if (KINESIS != null) {
            KINESIS.shutdown();
        }

        if (toxiproxy != null) {
            toxiproxy.stop();
        }
        if (localStack != null) {
            localStack.stop();
        }
    }

    @Test
    @Category(SerialTest.class)
    public void networkOutageWhenStarting() throws Exception {
        HELPER.createStream(10);

        System.err.println("Cutting network connection ...");
        setConnectionCut(PROXY, true);

        hz().getJet().newJob(getPipeline(kinesisSource().build()));
        Map<String, List<String>> expectedMessages = sendMessages();

        SECONDS.sleep(5);

        System.err.println("Network connection re-established");
        setConnectionCut(PROXY, false);

        assertMessages(expectedMessages, true, false);
    }

    @Test
    @Category(SerialTest.class)
    public void networkOutageWhileRunning() throws Exception {
        HELPER.createStream(10);

        hz().getJet().newJob(getPipeline(kinesisSource().build()));
        Map<String, List<String>> expectedMessages = sendMessages();

        //wait for some data to start coming out of the pipeline
        assertTrueEventually(() -> assertFalse(results.isEmpty()));

        System.err.println("Cutting network connection ...");
        setConnectionCut(PROXY, true);
        SECONDS.sleep(5);
        System.err.println("Network connection re-established");
        setConnectionCut(PROXY, false);

        assertMessages(expectedMessages, true, true);
        // duplication happens due AWS SDK internals (producer retries; details here:
        // https://docs.aws.amazon.com/streams/latest/dev/kinesis-record-processor-duplicates.html)
    }

    @Test
    @Category(SerialTest.class)
    @Ignore //AWS mock completely ignores the credentials passed to it, accepts anything (test passes on real backend)
    public void sinkWithIncorrectCredentials() {
        HELPER.createStream(1);

        AwsConfig awsConfig = new AwsConfig()
                .withCredentials("wrong_key", "wrong_key");
        Pipeline p = Pipeline.create();
        p.readFrom(TestSources.items(entry("k", new byte[0])))
                .writeTo(KinesisSinks.kinesis(STREAM)
                        .withEndpoint(awsConfig.getEndpoint())
                        .withRegion(awsConfig.getRegion())
                        .withCredentials(awsConfig.getAccessKey(), awsConfig.getSecretKey())
                        .withRetryStrategy(KinesisTestHelper.RETRY_STRATEGY)
                        .build());

        Job job = hz().getJet().newJob(p);
        assertThrowsJetException(job, "The security token included in the request is invalid");
    }

    @Test
    @Category(SerialTest.class)
    @Ignore //AWS mock completely ignores the credentials passed to it, accepts anything (test passes on real backend)
    public void sourceWithIncorrectCredentials() {
        HELPER.createStream(1);

        AwsConfig awsConfig = new AwsConfig()
                .withCredentials("wrong_key", "wrong_key");
        Pipeline p = Pipeline.create();
        p.readFrom(KinesisSources.kinesis(STREAM)
                    .withEndpoint(awsConfig.getEndpoint())
                    .withRegion(awsConfig.getRegion())
                    .withCredentials(awsConfig.getAccessKey(), awsConfig.getSecretKey())
                    .withRetryStrategy(KinesisTestHelper.RETRY_STRATEGY)
                    .build())
                .withoutTimestamps()
                .writeTo(Sinks.noop());

        Job job = hz().getJet().newJob(p);
        assertThrowsJetException(job, "The security token included in the request is invalid");
    }

    @Test
    @Category(SerialTest.class)
    public void keyTooShort() {
        Entry<String, byte[]> valid = entry("k", new byte[0]);
        Entry<String, byte[]> invalid = entry("", new byte[0]);
        invalidInputToSink(valid, invalid, "Key empty");
    }

    @Test
    @Category(SerialTest.class)
    public void keyTooLong() {
        Entry<String, byte[]> valid = entry("*".repeat(MAXIMUM_KEY_LENGTH), new byte[0]);
        Entry<String, byte[]> invalid = entry("*".repeat(MAXIMUM_KEY_LENGTH + 1), new byte[0]);
        invalidInputToSink(valid, invalid, "Key too long");
    }

    @Test
    @Category(SerialTest.class)
    public void valueTooLong() {
        Entry<String, byte[]> valid = entry("k", new byte[MAX_RECORD_SIZE - 1]);
        Entry<String, byte[]> invalid = entry("k", new byte[MAX_RECORD_SIZE]);
        invalidInputToSink(valid, invalid, "Encoded length (key + payload) is too big");
    }

    @Test
    @Category(SerialTest.class)
    public void valuePlusKeyTooLong() {
        Entry<String, byte[]> valid = entry("kkk", new byte[MAX_RECORD_SIZE - 3]);
        Entry<String, byte[]> invalid = entry("kkk", new byte[MAX_RECORD_SIZE - 2]);
        invalidInputToSink(valid, invalid, "Encoded length (key + payload) is too big");
    }

    private void invalidInputToSink(Entry<String, byte[]> valid, Entry<String, byte[]> invalid, String error) {
        HELPER.createStream(1);

        Job job1 = writeOneEntry(valid);
        job1.join();
        assertThat(job1).eventuallyHasStatus(JobStatus.COMPLETED);

        Job job2 = writeOneEntry(invalid);
        assertThrowsJetException(job2, error);
    }

    private Job writeOneEntry(Entry<String, byte[]> entry) {
        Pipeline p = Pipeline.create();
        p.readFrom(TestSources.items(entry))
                .writeTo(kinesisSink().build());

        return hz().getJet().newJob(p);
    }

    private static void assertThrowsJetException(Job job, String messageFragment) {
        try {
            job.getFuture().get(ASSERT_TRUE_EVENTUALLY_TIMEOUT, SECONDS);
        } catch (ExecutionException ee) {
            //job completed exceptionally, as expected, we check the details of it
            assertThat(ee)
                    .hasCauseInstanceOf(JetException.class)
                    .hasMessageContaining(messageFragment);
        } catch (Throwable t) {
            throw sneakyThrow(t);
        }
    }

    private void setConnectionCut(Proxy proxy, boolean shouldCutConnection) throws IOException {
        if (shouldCutConnection) {
            proxy.toxics().bandwidth(ToxicDirection.DOWNSTREAM.name(), ToxicDirection.DOWNSTREAM, 0);
            proxy.toxics().bandwidth(ToxicDirection.UPSTREAM.name(), ToxicDirection.UPSTREAM, 0);
        } else {
            proxy.toxics().get(ToxicDirection.DOWNSTREAM.name()).remove();
            proxy.toxics().get(ToxicDirection.UPSTREAM.name()).remove();
        }
    }

}
