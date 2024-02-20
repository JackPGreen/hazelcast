/*
 * Copyright 2024 Hazelcast Inc.
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

package com.hazelcast.jet.kafka.connect;

import com.hazelcast.collection.IList;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.core.JetTestSupport;
import com.hazelcast.jet.core.JobStatus;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.jet.pipeline.WindowDefinition;
import com.hazelcast.jet.pipeline.test.AssertionCompletedException;
import com.hazelcast.jet.pipeline.test.AssertionSinks;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.OverridePropertyRule;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.SlowTest;
import com.hazelcast.test.jdbc.MySQLDatabaseProvider;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CompletionException;

import static com.hazelcast.jet.core.JobStatus.FAILED;
import static com.hazelcast.jet.kafka.connect.TestUtil.getConnectorURL;
import static com.hazelcast.test.DockerTestUtil.assumeDockerEnabled;
import static com.hazelcast.test.OverridePropertyRule.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(HazelcastSerialClassRunner.class)
@Category({SlowTest.class, ParallelJVMTest.class})
public class KafkaConnectJdbcIT extends JetTestSupport {
    @ClassRule
    public static final OverridePropertyRule enableLogging = set("hazelcast.logging.type", "log4j2");

    public static final String USERNAME = "mysql";
    public static final String PASSWORD = "mysql";
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConnectJdbcIT.class);

    @SuppressWarnings("resource")
    @ClassRule
    public static final MySQLContainer<?> mysql = MySQLDatabaseProvider.createContainer()
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("Docker"));

    private static final int ITEM_COUNT = 1_000;
    private static final String FILE_NAME = "confluentinc-kafka-connect-jdbc-10.6.3.zip";

    @BeforeClass
    public static void setUpDocker() {
        assumeDockerEnabled();
    }


    @Test
    public void testReading() throws Exception {
        Properties randomProperties = new Properties();
        randomProperties.setProperty("name", "confluentinc-kafka-connect-jdbc");
        randomProperties.setProperty("connector.class", "io.confluent.connect.jdbc.JdbcSourceConnector");
        randomProperties.setProperty("mode", "incrementing");
        String connectionUrl = mysql.getJdbcUrl();
        randomProperties.setProperty("connection.url", connectionUrl);
        randomProperties.setProperty("connection.user", USERNAME);
        randomProperties.setProperty("connection.password", PASSWORD);
        randomProperties.setProperty("incrementing.column.name", "id");
        randomProperties.setProperty("table.whitelist", "items1");
        randomProperties.setProperty("table.poll.interval.ms", "5000");

        createTableAndFill(connectionUrl, "items1");


        Pipeline pipeline = Pipeline.create();
        StreamStage<String> streamStage = pipeline.readFrom(KafkaConnectSources.connect(randomProperties,
                        TestUtil::convertToString))
                .withoutTimestamps();
        streamStage.writeTo(Sinks.logger());
        streamStage
                .writeTo(AssertionSinks.assertCollectedEventually(60,
                        list -> assertEquals(ITEM_COUNT, list.size())));

        JobConfig jobConfig = new JobConfig();
        jobConfig.addJarsInZip(getConnectorURL(FILE_NAME));

        Config config = smallInstanceConfig();
        config.getJetConfig().setResourceUploadEnabled(true);
        Job job = createHazelcastInstance(config).getJet().newJob(pipeline, jobConfig);

        try {
            job.join();
            fail("Job should have completed with an AssertionCompletedException, but completed normally");
        } catch (CompletionException e) {
            String errorMsg = e.getCause().getMessage();
            assertTrue("Job was expected to complete with AssertionCompletedException, but completed with: "
                    + e.getCause(), errorMsg.contains(AssertionCompletedException.class.getName()));
        }
    }

    @Test
    public void testScaling() throws Exception {
        Properties randomProperties = new Properties();
        randomProperties.setProperty("name", "confluentinc-kafka-connect-jdbc");
        randomProperties.setProperty("tasks.max", "2");
        randomProperties.setProperty("connector.class", "io.confluent.connect.jdbc.JdbcSourceConnector");
        randomProperties.setProperty("mode", "incrementing");
        String connectionUrl = mysql.getJdbcUrl();
        randomProperties.setProperty("connection.url", connectionUrl);
        randomProperties.setProperty("connection.user", USERNAME);
        randomProperties.setProperty("connection.password", PASSWORD);
        randomProperties.setProperty("incrementing.column.name", "id");
        randomProperties.setProperty("table.whitelist", "parallel_items_1,parallel_items_2");
        randomProperties.setProperty("table.poll.interval.ms", "5000");

        createTableAndFill(connectionUrl, "parallel_items_1");
        createTableAndFill(connectionUrl, "parallel_items_2");


        Pipeline pipeline = Pipeline.create();
        StreamStage<String> streamStage = pipeline.readFrom(KafkaConnectSources.connect(randomProperties,
                        TestUtil::convertToString))
                .withoutTimestamps();
        streamStage.writeTo(Sinks.logger());
        streamStage
                .writeTo(AssertionSinks.assertCollectedEventually(60,
                        list -> assertEquals(2 * ITEM_COUNT, list.size())));

        JobConfig jobConfig = new JobConfig();
        jobConfig.addJarsInZip(getConnectorURL(FILE_NAME));

        Config config = smallInstanceConfig();
        config.getJetConfig().setResourceUploadEnabled(true);
        Job job = createHazelcastInstances(config, 3)[0].getJet().newJob(pipeline, jobConfig);

        try {
            job.join();
            fail("Job should have completed with an AssertionCompletedException, but completed normally");
        } catch (CompletionException e) {
            String errorMsg = e.getCause().getMessage();
            assertTrue("Job was expected to complete with AssertionCompletedException, but completed with: "
                    + e.getCause(), errorMsg.contains(AssertionCompletedException.class.getName()));
        }
    }

    @Test
    public void testScaling_with_new_member() throws Exception {
        Properties randomProperties = new Properties();
        randomProperties.setProperty("name", "confluentinc-kafka-connect-jdbc");
        randomProperties.setProperty("tasks.max", "2");
        randomProperties.setProperty("connector.class", "io.confluent.connect.jdbc.JdbcSourceConnector");
        randomProperties.setProperty("mode", "incrementing");
        String connectionUrl = mysql.getJdbcUrl();
        randomProperties.setProperty("connection.url", connectionUrl);
        randomProperties.setProperty("connection.user", USERNAME);
        randomProperties.setProperty("connection.password", PASSWORD);
        randomProperties.setProperty("incrementing.column.name", "id");
        randomProperties.setProperty("table.whitelist", "newmember_parallel_items_1,newmember_parallel_items_2");
        randomProperties.setProperty("table.poll.interval.ms", "5000");
        randomProperties.setProperty("batch.max.rows", "1");

        createTableAndFill(connectionUrl, "newmember_parallel_items_1");
        createTableAndFill(connectionUrl, "newmember_parallel_items_2");

        Config config = smallInstanceConfig();
        config.getJetConfig().setResourceUploadEnabled(true);
        HazelcastInstance hazelcastInstance1 = createHazelcastInstances(config, 1) [0];

        String listName = "destinationList";
        IList<Object> sinkList = hazelcastInstance1.getList(listName);

        // Create job
        Pipeline pipeline = Pipeline.create();
        StreamStage<String> streamStage = pipeline.readFrom(KafkaConnectSources.connect(randomProperties,
                        TestUtil::convertToString))
                .withoutTimestamps();
        streamStage.writeTo(Sinks.logger());
        streamStage.writeTo(Sinks.list(sinkList));

        JobConfig jobConfig = new JobConfig();
        jobConfig.setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE);
        jobConfig.addJarsInZip(getConnectorURL(FILE_NAME));

        Job job = hazelcastInstance1.getJet().newJob(pipeline, jobConfig);

        // There are some items in the list. The job is running
        assertTrueEventually(() -> assertTrue(sinkList.size() > 10));

        // Add one more member to  cluster. Job is going to auto-scale
        HazelcastInstance hazelcastInstance2 = createHazelcastInstances(config, 1)[0];
        assertClusterSizeEventually(2, hazelcastInstance1, hazelcastInstance2);

        // We should see more items in the list
        assertTrueEventually(() -> assertTrue(sinkList.size() >= 2 * ITEM_COUNT));
    }

    @Test
    public void testDynamicReconfiguration() throws Exception {
        Properties randomProperties = new Properties();
        randomProperties.setProperty("name", "confluentinc-kafka-connect-jdbc");
        randomProperties.setProperty("connector.class", "io.confluent.connect.jdbc.JdbcSourceConnector");
        randomProperties.setProperty("mode", "incrementing");
        String connectionUrl = mysql.getJdbcUrl();
        randomProperties.setProperty("connection.url", connectionUrl);
        randomProperties.setProperty("connection.user", USERNAME);
        randomProperties.setProperty("connection.password", PASSWORD);
        randomProperties.setProperty("incrementing.column.name", "id");
        randomProperties.setProperty("table.whitelist", "dynamic_test_items1,dynamic_test_items2,dynamic_test_items3");
        randomProperties.setProperty("table.poll.interval.ms", "1000");

        createTableAndFill(connectionUrl, "dynamic_test_items1");

        Pipeline pipeline = Pipeline.create();
        StreamStage<String> streamStage = pipeline.readFrom(KafkaConnectSources.connect(randomProperties,
                        TestUtil::convertToString))
                .withoutTimestamps()
                .setLocalParallelism(1);
        streamStage.writeTo(Sinks.logger());
        streamStage
                .writeTo(AssertionSinks.assertCollectedEventually(60,
                        list -> assertEquals(3 * ITEM_COUNT, list.size())));

        JobConfig jobConfig = new JobConfig();
        jobConfig.addJarsInZip(getConnectorURL(FILE_NAME));

        Config config = smallInstanceConfig();
        config.getJetConfig().setResourceUploadEnabled(true);
        HazelcastInstance hazelcastInstance = createHazelcastInstance(config);
        Job job = hazelcastInstance.getJet().newJob(pipeline, jobConfig);

        createTableAndFill(connectionUrl, "dynamic_test_items2");
        createTableAndFill(connectionUrl, "dynamic_test_items3");

        try {
            job.join();
            fail("Job should have completed with an AssertionCompletedException, but completed normally");
        } catch (CompletionException e) {
            String errorMsg = e.getCause().getMessage();
            assertTrue("Job was expected to complete with AssertionCompletedException, but completed with: "
                    + e.getCause(), errorMsg.contains(AssertionCompletedException.class.getName()));
        }
    }

    private void createTableAndFill(String connectionUrl, String tableName) throws SQLException {
        createTable(connectionUrl, tableName);
        insertItems(connectionUrl, tableName);
    }

    private static void insertItems(String connectionUrl, String tableName) throws SQLException {
        try (Connection conn = DriverManager.getConnection(connectionUrl, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()
        ) {
            for (int i = 0; i < ITEM_COUNT; i++) {
                String sql = String.format("INSERT INTO " + tableName + " VALUES(%d, '" + tableName + "-%d')", i, i);
                stmt.addBatch(sql);
            }
            int[] ints = stmt.executeBatch();
            boolean allMatch = Arrays.stream(ints).allMatch(i -> i == 1);
            assertTrue(allMatch);
        }
    }

    private static void createTable(String connectionUrl, String tableName) throws SQLException {
        try (Connection conn = DriverManager.getConnection(connectionUrl, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()
        ) {
            stmt.execute("CREATE TABLE " + tableName + " (id INT PRIMARY KEY, name VARCHAR(128))");
        }
    }

    @Test
    public void windowing_withIngestionTimestamps_should_work() throws Exception {
        Properties randomProperties = new Properties();
        randomProperties.setProperty("name", "confluentinc-kafka-connect-jdbc");
        randomProperties.setProperty("connector.class", "io.confluent.connect.jdbc.JdbcSourceConnector");
        randomProperties.setProperty("mode", "incrementing");
        String connectionUrl = mysql.getJdbcUrl();
        randomProperties.setProperty("connection.url", connectionUrl);
        randomProperties.setProperty("connection.user", USERNAME);
        randomProperties.setProperty("connection.password", PASSWORD);
        randomProperties.setProperty("incrementing.column.name", "id");
        randomProperties.setProperty("table.whitelist", "windowing_test");
        randomProperties.setProperty("table.poll.interval.ms", "1000");

        createTable(connectionUrl, "windowing_test");

        final Pipeline pipeline = Pipeline.create();
        StreamStage<Long> streamStage = pipeline.readFrom(KafkaConnectSources.connect(randomProperties,
                        TestUtil::convertToString))
                .withIngestionTimestamps()
                .setLocalParallelism(1)
                .window(WindowDefinition.tumbling(10))
                .distinct()
                .rollingAggregate(AggregateOperations.counting());
        streamStage.writeTo(Sinks.logger());
        streamStage.writeTo(Sinks.list("windowing_test_results"));
        JobConfig jobConfig = new JobConfig();
        jobConfig.addJarsInZip(getConnectorURL(FILE_NAME));
        Config config = smallInstanceConfig();
        config.getJetConfig().setResourceUploadEnabled(true);
        HazelcastInstance hazelcastInstance = createHazelcastInstance(config);
        Job job = hazelcastInstance.getJet().newJob(pipeline, jobConfig);
        assertJobStatusEventually(job, JobStatus.RUNNING);

        insertItems(connectionUrl, "windowing_test");

        IList<Long> list = hazelcastInstance.getList("windowing_test_results");
        assertTrueEventually(() -> Assertions.assertThat(list)
                .isNotEmpty()
                .last().isEqualTo((long) ITEM_COUNT));

        job.cancel();
        assertJobStatusEventually(job, FAILED);
    }

}