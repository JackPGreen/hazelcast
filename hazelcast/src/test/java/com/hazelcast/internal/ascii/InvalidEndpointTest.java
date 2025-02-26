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

package com.hazelcast.internal.ascii;

import com.hazelcast.config.Config;
import com.hazelcast.config.RestServerEndpointConfig;
import com.hazelcast.config.ServerSocketEndpointConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.TestAwareInstanceFactory;
import com.hazelcast.test.annotation.SlowTest;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static com.hazelcast.instance.EndpointQualifier.MEMCACHE;
import static com.hazelcast.instance.EndpointQualifier.REST;
import static com.hazelcast.test.HazelcastTestSupport.ignore;
import static com.hazelcast.test.MemcacheTestUtil.shutdownQuietly;
import static org.junit.Assert.fail;

@RunWith(HazelcastSerialClassRunner.class)
@Category(SlowTest.class)
public class InvalidEndpointTest {

    protected final TestAwareInstanceFactory factory = new TestAwareInstanceFactory();

    @After
    public void tearDown() {
        factory.terminateAll();
    }

    @Test
    public void attemptHttpOnMemcacheEndpoint() {
        Config config = createMemcacheEndpointConfig();
        HazelcastInstance instance = factory.newHazelcastInstance(config);

        // Invalid endpoint - points to MEMCACHE
        String address = instance.getCluster().getLocalMember().getSocketAddress(MEMCACHE).toString();
        String url = "http:/" + address + "/management/cluster/version";
        try {
            sendGet(url);
            fail("Should fail with message defined in com.hazelcast.internal.nio.Protocols.UNEXPECTED_PROTOCOL");
        } catch (IOException | InterruptedException e) {
            ignore(e);
        }
    }

    protected Config createMemcacheEndpointConfig() {
        ServerSocketEndpointConfig endpointConfig = new ServerSocketEndpointConfig();
        endpointConfig.setName("Text")
                    .setPort(10000)
                    .setPortAutoIncrement(true);

        Config config = new Config();
        config.getAdvancedNetworkConfig()
              .setMemcacheEndpointConfig(endpointConfig)
              .setEnabled(true);
        return config;
    }

    @Test
    public void attemptMemcacheOnHttpEndpoint()
            throws IOException, InterruptedException {
        Config config = createRestEndpointConfig();
        HazelcastInstance instance = factory.newHazelcastInstance(config);

        // Invalid endpoint - points to REST
        InetSocketAddress address = instance.getCluster().getLocalMember().getSocketAddress(REST);
        ConnectionFactory factory = new ConnectionFactoryBuilder()
                .setOpTimeout(60 * 60 * 60)
                .setDaemon(true)
                .setFailureMode(FailureMode.Retry)
                .build();
        MemcachedClient client = new MemcachedClient(factory, Collections.singletonList(address));

        try {
            client.set("one", 0, "two").get();
            fail("Should not be able to connect");
        } catch (InterruptedException | ExecutionException e) {
            ignore(e);
        }

        shutdownQuietly(client);
    }

    protected Config createRestEndpointConfig() {
        RestServerEndpointConfig restEndpoint = new RestServerEndpointConfig();
        restEndpoint.setName("Text")
                    .setPort(10000)
                    .setPortAutoIncrement(true)
                    .enableAllGroups();

        Config config = new Config();
        config.getAdvancedNetworkConfig()
              .setRestEndpointConfig(restEndpoint)
              .setEnabled(true);
        return config;
    }

    protected Object doHttpGet() throws IOException {
        return null;
    }

    protected Object newHttpClient() throws IOException {
        return null;
    }

    protected void sendGet(String url) throws IOException, InterruptedException {
        HttpClient client = createHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        client.send(request, HttpResponse.BodyHandlers.discarding());
    }

    protected HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }
}
