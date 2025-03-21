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

package com.hazelcast.spring;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.LoadBalancer;
import com.hazelcast.client.config.ClientCloudConfig;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientConnectionStrategyConfig;
import com.hazelcast.client.config.ClientConnectionStrategyConfig.ReconnectMode;
import com.hazelcast.client.config.ClientFlakeIdGeneratorConfig;
import com.hazelcast.client.config.ClientIcmpPingConfig;
import com.hazelcast.client.config.ClientMetricsConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.ClientReliableTopicConfig;
import com.hazelcast.client.config.ClientSqlConfig;
import com.hazelcast.client.config.ClientSqlResubmissionMode;
import com.hazelcast.client.config.ClientTpcConfig;
import com.hazelcast.client.config.ClientUserCodeDeploymentConfig;
import com.hazelcast.client.config.ConnectionRetryConfig;
import com.hazelcast.client.config.ProxyFactoryConfig;
import com.hazelcast.client.impl.clientside.HazelcastClientProxy;
import com.hazelcast.client.util.RoundRobinLB;
import com.hazelcast.collection.IList;
import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ISet;
import com.hazelcast.config.AwsConfig;
import com.hazelcast.config.CompactSerializationConfig;
import com.hazelcast.config.CompactSerializationConfigAccessor;
import com.hazelcast.config.EntryListenerConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.IndexConfig;
import com.hazelcast.config.IndexType;
import com.hazelcast.config.InstanceTrackingConfig;
import com.hazelcast.config.LoginModuleConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.hazelcast.config.NativeMemoryConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.config.NearCachePreloaderConfig;
import com.hazelcast.config.PersistentMemoryConfig;
import com.hazelcast.config.PersistentMemoryDirectoryConfig;
import com.hazelcast.config.QueryCacheConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.config.security.JaasAuthenticationConfig;
import com.hazelcast.config.security.RealmConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.memory.MemoryUnit;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.security.Credentials;
import com.hazelcast.spring.serialization.DummyCompactSerializer;
import com.hazelcast.spring.serialization.DummyReflectiveSerializable;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.TopicOverloadPolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.ByteOrder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.hazelcast.config.NearCacheConfig.LocalUpdatePolicy.CACHE_ON_UPDATE;
import static com.hazelcast.config.PersistentMemoryMode.MOUNTED;
import static com.hazelcast.config.PersistentMemoryMode.SYSTEM_MEMORY;
import static com.hazelcast.test.HazelcastTestSupport.assertContains;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith({SpringExtension.class, CustomSpringExtension.class})
@ContextConfiguration(locations = {"node-client-applicationContext-hazelcast.xml"})
class TestClientApplicationContext {

    @Autowired
    @Qualifier(value = "client")
    private HazelcastClientProxy client;

    @Autowired
    @Qualifier(value = "client2")
    private HazelcastClientProxy client2;

    @Autowired
    @Qualifier(value = "client3")
    private HazelcastClientProxy client3;

    @Autowired
    @Qualifier(value = "client4")
    private HazelcastClientProxy client4;

    @Autowired
    @Qualifier(value = "client5")
    private HazelcastClientProxy client5;

    @Autowired
    @Qualifier(value = "client6")
    private HazelcastClientProxy client6;

    @Autowired
    @Qualifier(value = "client7-empty-serialization-config")
    private HazelcastClientProxy client7;

    @Autowired
    @Qualifier(value = "client8")
    private HazelcastClientProxy client8;

    @Autowired
    @Qualifier(value = "client9-user-code-deployment-test")
    private HazelcastClientProxy userCodeDeploymentTestClient;

    @Autowired
    @Qualifier(value = "client10-flakeIdGenerator")
    private HazelcastClientProxy client10;

    @Autowired
    @Qualifier(value = "client11-icmp-ping")
    private HazelcastClientProxy icmpPingTestClient;

    @Autowired
    @Qualifier(value = "client12-hazelcast-cloud")
    private HazelcastClientProxy hazelcastCloudClient;

    @Autowired
    @Qualifier(value = "client13-exponential-connection-retry")
    private HazelcastClientProxy connectionRetryClient;

    @Autowired
    @Qualifier(value = "client14-reliable-topic")
    private HazelcastClientProxy hazelcastReliableTopic;

    @Autowired
    @Qualifier(value = "client16-name-and-labels")
    private HazelcastClientProxy namedClient;

    @Autowired
    @Qualifier(value = "client17-backupAckToClient")
    private HazelcastClientProxy backupAckToClient;

    @Autowired
    @Qualifier(value = "client18-metrics")
    private HazelcastClientProxy metricsClient;

    @Autowired
    @Qualifier(value = "client19-instance-tracking")
    private HazelcastClientProxy instanceTrackingClient;

    @Autowired
    @Qualifier(value = "client20-native-memory")
    private HazelcastClientProxy nativeMemoryClient;

    @Autowired
    @Qualifier(value = "client21-persistent-memory-system-memory")
    private HazelcastClientProxy pmemSystemMemoryClient;

    @Autowired
    @Qualifier(value = "client22-with-overridden-default-serializers")
    private HazelcastClientProxy clientWithOverriddenDefaultSerializers;

    @Autowired
    @Qualifier(value = "client23-with-compact-serialization")
    private HazelcastClientProxy clientWithCompactSerialization;

    @Autowired
    @Qualifier(value = "client24-with-sql")
    private HazelcastClientProxy clientWithSql;

    @Autowired
    @Qualifier(value = "client25-with-tpc")
    private HazelcastClientProxy clientWithTpc;

    @Autowired
    @Qualifier(value = "instance")
    private HazelcastInstance instance;

    @Autowired
    @Qualifier(value = "map1")
    private IMap<Object, Object> map1;

    @Autowired
    @Qualifier(value = "map2")
    private IMap<Object, Object> map2;

    @Autowired
    @Qualifier(value = "multiMap")
    private MultiMap<?, ?> multiMap;

    @Autowired
    @Qualifier(value = "queue")
    private IQueue<?> queue;

    @Autowired
    @Qualifier(value = "topic")
    private ITopic<?> topic;

    @Autowired
    @Qualifier(value = "set")
    private ISet<?> set;

    @Autowired
    @Qualifier(value = "list")
    private IList<?> list;

    @Autowired
    @Qualifier(value = "executorService")
    private ExecutorService executorService;

    @Autowired
    @Qualifier(value = "reliableTopic")
    private ITopic<?> reliableTopic;

    @Autowired
    private Credentials credentials;

    @BeforeAll
    @AfterAll
    static void start() {
        HazelcastClient.shutdownAll();
        Hazelcast.shutdownAll();
    }

    @Test
    void testClient() {
        assertNotNull(client);
        assertNotNull(client2);
        assertNotNull(client3);

        ClientConfig config = client.getClientConfig();
        assertEquals("13", config.getProperty("hazelcast.client.retry.count"));
        assertEquals(1000, config.getNetworkConfig().getConnectionTimeout());

        client.getMap("default").put("Q", "q");
        client2.getMap("default").put("X", "x");

        IMap<Object, Object> map = instance.getMap("default");
        assertEquals("q", map.get("Q"));
        assertEquals("x", map.get("X"));

        ClientConfig config3 = client3.getClientConfig();
        SerializationConfig serConf = config3.getSerializationConfig();

        assertEquals(ByteOrder.BIG_ENDIAN, serConf.getByteOrder());
        assertFalse(serConf.isAllowUnsafe());
        assertFalse(serConf.isCheckClassDefErrors());
        assertFalse(serConf.isEnableCompression());
        assertFalse(serConf.isEnableSharedObject());
        assertFalse(serConf.isUseNativeByteOrder());
        assertEquals(10, serConf.getPortableVersion());

        Map<Integer, String> dataSerializableFactoryClasses = serConf.getDataSerializableFactoryClasses();
        assertNotNull(dataSerializableFactoryClasses);
        assertTrue(dataSerializableFactoryClasses.containsKey(1));
        assertEquals("com.hazelcast.spring.serialization.DummyDataSerializableFactory", dataSerializableFactoryClasses.get(1));

        Map<Integer, String> portableFactoryClasses = serConf.getPortableFactoryClasses();
        assertNotNull(portableFactoryClasses);
        assertTrue(portableFactoryClasses.containsKey(2));
        assertEquals("com.hazelcast.spring.serialization.DummyPortableFactory", portableFactoryClasses.get(2));

        Collection<SerializerConfig> serializerConfigs = serConf.getSerializerConfigs();
        assertNotNull(serializerConfigs);

        SerializerConfig serializerConfig = serializerConfigs.iterator().next();
        assertNotNull(serializerConfig);
        assertEquals("com.hazelcast.internal.serialization.impl.CustomSerializationTest$FooXmlSerializer",
                serializerConfig.getClassName());
        assertEquals("com.hazelcast.internal.serialization.impl.CustomSerializationTest$Foo",
                serializerConfig.getTypeClassName());

        List<ProxyFactoryConfig> proxyFactoryConfigs = config3.getProxyFactoryConfigs();
        assertNotNull(proxyFactoryConfigs);
        ProxyFactoryConfig proxyFactoryConfig = proxyFactoryConfigs.get(0);
        assertNotNull(proxyFactoryConfig);
        assertEquals("com.hazelcast.spring.DummyProxyFactory", proxyFactoryConfig.getClassName());
        assertEquals("MyService", proxyFactoryConfig.getService());

        LoadBalancer loadBalancer = config3.getLoadBalancer();
        assertNotNull(loadBalancer);
        assertInstanceOf(RoundRobinLB.class, loadBalancer);

        NearCacheConfig nearCacheConfig = config3.getNearCacheConfig("default");
        assertNotNull(nearCacheConfig);

        assertEquals(1, nearCacheConfig.getTimeToLiveSeconds());
        assertEquals(70, nearCacheConfig.getMaxIdleSeconds());
        assertEquals(EvictionPolicy.LRU, nearCacheConfig.getEvictionConfig().getEvictionPolicy());
        assertEquals(4000, nearCacheConfig.getEvictionConfig().getSize());
        assertTrue(nearCacheConfig.isInvalidateOnChange());
        assertFalse(nearCacheConfig.isSerializeKeys());
        assertEquals(CACHE_ON_UPDATE, nearCacheConfig.getLocalUpdatePolicy());
    }

    @Test
    void testAwsClientConfig() {
        assertNotNull(client4);
        ClientConfig config = client4.getClientConfig();
        ClientNetworkConfig networkConfig = config.getNetworkConfig();

        AwsConfig awsConfig = networkConfig.getAwsConfig();
        assertFalse(awsConfig.isEnabled());
        assertTrue(awsConfig.isUsePublicIp());
        assertEquals("sample-access-key", awsConfig.getProperty("access-key"));
        assertEquals("sample-secret-key", awsConfig.getProperty("secret-key"));
        assertEquals("sample-region", awsConfig.getProperty("region"));
        assertEquals("sample-group", awsConfig.getProperty("security-group-name"));
        assertEquals("sample-tag-key", awsConfig.getProperty("tag-key"));
        assertEquals("sample-tag-value", awsConfig.getProperty("tag-value"));
    }

    @Test
    void testUnlimitedConnectionAttempt() {
        assertNotNull(client5);

        ClientConfig config = client5.getClientConfig();
        assertEquals(1000, config.getConnectionStrategyConfig().getConnectionRetryConfig().getClusterConnectTimeoutMillis());
    }

    @Test
    void testSecurityRealms() {
        assertNotNull(client5);

        RealmConfig realmConfig = client5.getClientConfig().getSecurityConfig().getRealmConfig("krb5Initiator");
        assertNotNull(realmConfig);
        JaasAuthenticationConfig jaasAuthenticationConfig = realmConfig.getJaasAuthenticationConfig();
        assertNotNull(jaasAuthenticationConfig);
        assertEquals(1, jaasAuthenticationConfig.getLoginModuleConfigs().size());
        LoginModuleConfig loginModuleConfig = jaasAuthenticationConfig.getLoginModuleConfigs().get(0);
        assertEquals("com.sun.security.auth.module.Krb5LoginModule", loginModuleConfig.getClassName());
        assertEquals("jduke@HAZELCAST.COM", loginModuleConfig.getProperties().getProperty("principal"));
    }

    @Test
    void testHazelcastInstances() {
        assertNotNull(map1);
        assertNotNull(map2);
        assertNotNull(multiMap);
        assertNotNull(queue);
        assertNotNull(topic);
        assertNotNull(set);
        assertNotNull(list);
        assertNotNull(executorService);
        assertNotNull(reliableTopic);
        assertEquals("map1", map1.getName());
        assertEquals("map2", map2.getName());
        assertEquals("multiMap", multiMap.getName());
        assertEquals("queue", queue.getName());
        assertEquals("topic", topic.getName());
        assertEquals("set", set.getName());
        assertEquals("list", list.getName());
        assertEquals("reliableTopic", reliableTopic.getName());
    }

    @Test
    void testDefaultSerializationConfig() {
        ClientConfig config7 = client7.getClientConfig();
        SerializationConfig serConf = config7.getSerializationConfig();

        assertEquals(ByteOrder.BIG_ENDIAN, serConf.getByteOrder());
        assertFalse(serConf.isAllowUnsafe());
        assertFalse(serConf.isAllowOverrideDefaultSerializers());
        assertTrue(serConf.isCheckClassDefErrors());
        assertFalse(serConf.isEnableCompression());
        assertTrue(serConf.isEnableSharedObject());
        assertFalse(serConf.isUseNativeByteOrder());
        assertEquals(0, serConf.getPortableVersion());
    }

    @Test
    void testOverrideDefaultSerializersSerializationConfig() {
        final ClientConfig config = clientWithOverriddenDefaultSerializers.getClientConfig();
        final SerializationConfig serializationConfig = config.getSerializationConfig();

        assertTrue(serializationConfig.isAllowOverrideDefaultSerializers());
    }

    @Test
    void tesCompactSerializationConfig() {
        CompactSerializationConfig compactSerializationConfig = clientWithCompactSerialization.getClientConfig()
                .getSerializationConfig()
                .getCompactSerializationConfig();

        List<String> serializerClassNames
                = CompactSerializationConfigAccessor.getSerializerClassNames(compactSerializationConfig);
        assertEquals(1, serializerClassNames.size());

        List<String> compactSerializableClassNames
                = CompactSerializationConfigAccessor.getCompactSerializableClassNames(compactSerializationConfig);
        assertEquals(1, compactSerializableClassNames.size());

        String reflectivelySerializableClassName = DummyReflectiveSerializable.class.getName();
        assertThat(compactSerializableClassNames)
                .contains(reflectivelySerializableClassName);

        String compactSerializerClassName = DummyCompactSerializer.class.getName();
        assertThat(serializerClassNames)
                .contains(compactSerializerClassName);
    }

    @Test
    void testClientNearCacheEvictionPolicies() {
        ClientConfig config = client3.getClientConfig();
        assertEquals(EvictionPolicy.LFU, getNearCacheEvictionPolicy("lfuNearCacheEviction", config));
        assertEquals(EvictionPolicy.LRU, getNearCacheEvictionPolicy("lruNearCacheEviction", config));
        assertEquals(EvictionPolicy.RANDOM, getNearCacheEvictionPolicy("randomNearCacheEviction", config));
        assertEquals(EvictionPolicy.NONE, getNearCacheEvictionPolicy("noneNearCacheEviction", config));
    }

    @Test
    void testNearCachePreloader() {
        NearCachePreloaderConfig preloaderConfig = client3.getClientConfig()
                .getNearCacheConfig("preloader")
                .getPreloaderConfig();

        assertTrue(preloaderConfig.isEnabled());
        assertEquals("/tmp/preloader", preloaderConfig.getDirectory());
        assertEquals(23, preloaderConfig.getStoreInitialDelaySeconds());
        assertEquals(42, preloaderConfig.getStoreIntervalSeconds());
    }

    @Test
    void testUserCodeDeploymentConfig() {
        ClientConfig config = userCodeDeploymentTestClient.getClientConfig();
        ClientUserCodeDeploymentConfig userCodeDeploymentConfig = config.getUserCodeDeploymentConfig();
        List<String> classNames = userCodeDeploymentConfig.getClassNames();
        assertFalse(userCodeDeploymentConfig.isEnabled());
        assertEquals(2, classNames.size());
        assertTrue(classNames.contains("SampleClassName1"));
        assertTrue(classNames.contains("SampleClassName2"));
        List<String> jarPaths = userCodeDeploymentConfig.getJarPaths();
        assertEquals(1, jarPaths.size());
        assertTrue(jarPaths.contains("/User/jar/path/test.jar"));
    }

    private EvictionPolicy getNearCacheEvictionPolicy(String mapName, ClientConfig clientConfig) {
        return clientConfig.getNearCacheConfig(mapName).getEvictionConfig().getEvictionPolicy();
    }

    @Test
    void testFullQueryCacheConfig() {
        ClientConfig config = client6.getClientConfig();

        QueryCacheConfig queryCacheConfig = getQueryCacheConfig(config);
        assert queryCacheConfig != null;
        EntryListenerConfig entryListenerConfig = queryCacheConfig.getEntryListenerConfigs().get(0);

        assertTrue(entryListenerConfig.isIncludeValue());
        assertFalse(entryListenerConfig.isLocal());

        assertEquals("com.hazelcast.spring.DummyEntryListener", entryListenerConfig.getClassName());
        assertFalse(queryCacheConfig.isIncludeValue());

        assertEquals("my-query-cache-1", queryCacheConfig.getName());
        assertEquals(12, queryCacheConfig.getBatchSize());
        assertEquals(33, queryCacheConfig.getBufferSize());
        assertEquals(12, queryCacheConfig.getDelaySeconds());
        assertEquals(InMemoryFormat.OBJECT, queryCacheConfig.getInMemoryFormat());
        assertTrue(queryCacheConfig.isCoalesce());
        assertFalse(queryCacheConfig.isPopulate());
        assertEquals("__key > 12", queryCacheConfig.getPredicateConfig().getSql());
        assertEquals(EvictionPolicy.LRU, queryCacheConfig.getEvictionConfig().getEvictionPolicy());
        assertEquals(MaxSizePolicy.ENTRY_COUNT, queryCacheConfig.getEvictionConfig().getMaxSizePolicy());
        assertEquals(111, queryCacheConfig.getEvictionConfig().getSize());

        assertEquals(2, queryCacheConfig.getIndexConfigs().size());

        IndexConfig hashIndex = queryCacheConfig.getIndexConfigs().get(0);
        assertEquals(IndexType.HASH, hashIndex.getType());
        assertNull(hashIndex.getName());
        assertEquals(1, hashIndex.getAttributes().size());
        assertEquals("name", hashIndex.getAttributes().get(0));

        IndexConfig sortedIndex = queryCacheConfig.getIndexConfigs().get(1);
        assertEquals(IndexType.SORTED, sortedIndex.getType());
        assertEquals("sortedIndex", sortedIndex.getName());
        assertEquals(2, sortedIndex.getAttributes().size());
        assertEquals("age", sortedIndex.getAttributes().get(0));
        assertEquals("name", sortedIndex.getAttributes().get(1));
    }

    @Test
    void testClientConnectionStrategyConfig() {
        ClientConnectionStrategyConfig connectionStrategyConfig = client8.getClientConfig().getConnectionStrategyConfig();
        assertTrue(connectionStrategyConfig.isAsyncStart());
        assertEquals(ReconnectMode.ASYNC, connectionStrategyConfig.getReconnectMode());
    }

    @Test
    void testFlakeIdGeneratorConfig() {
        Map<String, ClientFlakeIdGeneratorConfig> configMap = client10.getClientConfig().getFlakeIdGeneratorConfigMap();
        assertEquals(1, configMap.size());
        ClientFlakeIdGeneratorConfig config = configMap.values().iterator().next();
        assertEquals("gen1", config.getName());
        assertEquals(3, config.getPrefetchCount());
        assertEquals(3000L, config.getPrefetchValidityMillis());
    }

    @Test
    void testClientIcmpConfig() {
        ClientIcmpPingConfig icmpPingConfig = icmpPingTestClient.getClientConfig()
                .getNetworkConfig().getClientIcmpPingConfig();
        assertFalse(icmpPingConfig.isEnabled());
        assertEquals(2000, icmpPingConfig.getTimeoutMilliseconds());
        assertEquals(3000, icmpPingConfig.getIntervalMilliseconds());
        assertEquals(50, icmpPingConfig.getTtl());
        assertEquals(5, icmpPingConfig.getMaxAttempts());
        assertFalse(icmpPingConfig.isEchoFailFastOnStartup());
    }

    @Test
    void testCloudConfig() {
        ClientCloudConfig cloudConfig = hazelcastCloudClient.getClientConfig()
                .getNetworkConfig().getCloudConfig();
        assertFalse(cloudConfig.isEnabled());
        assertEquals("EXAMPLE_TOKEN", cloudConfig.getDiscoveryToken());
    }

    @Test
    void testConnectionRetry() {
        ConnectionRetryConfig connectionRetryConfig = connectionRetryClient
                .getClientConfig().getConnectionStrategyConfig().getConnectionRetryConfig();
        assertEquals(5000, connectionRetryConfig.getClusterConnectTimeoutMillis());
        assertEquals(0.5, connectionRetryConfig.getJitter(), 0);
        assertEquals(2000, connectionRetryConfig.getInitialBackoffMillis());
        assertEquals(60000, connectionRetryConfig.getMaxBackoffMillis());
        assertEquals(3, connectionRetryConfig.getMultiplier(), 0);
    }

    @Test
    void testReliableTopicConfig() {
        ClientConfig clientConfig = hazelcastReliableTopic.getClientConfig();
        ClientReliableTopicConfig topicConfig = clientConfig.getReliableTopicConfig("rel-topic");
        assertEquals(100, topicConfig.getReadBatchSize());
        assertEquals(TopicOverloadPolicy.DISCARD_NEWEST, topicConfig.getTopicOverloadPolicy());
    }

    private static QueryCacheConfig getQueryCacheConfig(ClientConfig config) {
        Map<String, Map<String, QueryCacheConfig>> queryCacheConfigs = config.getQueryCacheConfigs();
        Collection<Map<String, QueryCacheConfig>> values = queryCacheConfigs.values();
        for (Map<String, QueryCacheConfig> value : values) {
            Set<Map.Entry<String, QueryCacheConfig>> entries = value.entrySet();
            for (Map.Entry<String, QueryCacheConfig> entry : entries) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Test
    void testInstanceNameConfig() {
        assertEquals("clusterName", namedClient.getName());
    }

    @Test
    void testLabelsConfig() {
        Set<String> labels = namedClient.getClientConfig().getLabels();
        assertEquals(1, labels.size());
        assertContains(labels, "foo");
    }

    @Test
    void testBackupAckToClient() {
        assertFalse(backupAckToClient.getClientConfig().isBackupAckToClientEnabled());
    }

    @Test
    void testMetrics() {
        ClientMetricsConfig metricsConfig = metricsClient.getClientConfig().getMetricsConfig();

        assertFalse(metricsConfig.isEnabled());
        assertFalse(metricsConfig.getJmxConfig().isEnabled());
        assertEquals(42, metricsConfig.getCollectionFrequencySeconds());
    }

    @Test
    void testInstanceTracking() {
        InstanceTrackingConfig trackingConfig = instanceTrackingClient.getClientConfig().getInstanceTrackingConfig();

        assertTrue(trackingConfig.isEnabled());
        assertEquals("/dummy/file", trackingConfig.getFileName());
        assertEquals("dummy-pattern with $HZ_INSTANCE_TRACKING{placeholder} and $RND{placeholder}",
                trackingConfig.getFormatPattern());
    }

    @Test
    void testNativeMemory() {
        NativeMemoryConfig nativeMemoryConfig = nativeMemoryClient.getClientConfig().getNativeMemoryConfig();

        assertFalse(nativeMemoryConfig.isEnabled());
        assertEquals(MemoryUnit.GIGABYTES, nativeMemoryConfig.getCapacity().getUnit());
        assertEquals(256, nativeMemoryConfig.getCapacity().getValue());
        assertEquals(20, nativeMemoryConfig.getPageSize());
        assertEquals(NativeMemoryConfig.MemoryAllocatorType.STANDARD, nativeMemoryConfig.getAllocatorType());
        assertEquals(10.2, nativeMemoryConfig.getMetadataSpacePercentage(), 0.1);
        assertEquals(10, nativeMemoryConfig.getMinBlockSize());

        PersistentMemoryConfig pmemConfig = nativeMemoryConfig.getPersistentMemoryConfig();
        assertFalse(pmemConfig.isEnabled());
        assertEquals(MOUNTED, pmemConfig.getMode());

        List<PersistentMemoryDirectoryConfig> directoryConfigs = pmemConfig.getDirectoryConfigs();
        assertEquals(2, directoryConfigs.size());
        assertEquals("/mnt/pmem0", directoryConfigs.get(0).getDirectory());
        assertEquals(0, directoryConfigs.get(0).getNumaNode());
        assertEquals("/mnt/pmem1", directoryConfigs.get(1).getDirectory());
        assertEquals(1, directoryConfigs.get(1).getNumaNode());
    }

    @Test
    void testNativeMemorySystemMemory() {
        NativeMemoryConfig nativeMemoryConfig = pmemSystemMemoryClient.getClientConfig().getNativeMemoryConfig();

        assertFalse(nativeMemoryConfig.isEnabled());
        assertEquals(MemoryUnit.GIGABYTES, nativeMemoryConfig.getCapacity().getUnit());
        assertEquals(256, nativeMemoryConfig.getCapacity().getValue());
        assertEquals(20, nativeMemoryConfig.getPageSize());
        assertEquals(NativeMemoryConfig.MemoryAllocatorType.STANDARD, nativeMemoryConfig.getAllocatorType());
        assertEquals(10.2, nativeMemoryConfig.getMetadataSpacePercentage(), 0.1);
        assertEquals(10, nativeMemoryConfig.getMinBlockSize());

        PersistentMemoryConfig pmemConfig = nativeMemoryConfig.getPersistentMemoryConfig();
        assertTrue(pmemConfig.isEnabled());
        assertEquals(SYSTEM_MEMORY, pmemConfig.getMode());
    }

    @Test
    void testSql() {
        ClientSqlConfig sqlConfig = clientWithSql.getClientConfig().getSqlConfig();
        assertEquals(ClientSqlResubmissionMode.RETRY_SELECTS, sqlConfig.getResubmissionMode());
    }

    @Test
    void testTpc() {
        ClientTpcConfig tpcConfig = clientWithTpc.getClientConfig().getTpcConfig();
        assertTrue(tpcConfig.isEnabled());
    }
}
