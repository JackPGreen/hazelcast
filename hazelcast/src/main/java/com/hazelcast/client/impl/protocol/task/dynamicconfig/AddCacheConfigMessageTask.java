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

package com.hazelcast.client.impl.protocol.task.dynamicconfig;

import com.hazelcast.client.impl.ClientEngine;
import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.DynamicConfigAddCacheConfigCodec;
import com.hazelcast.config.CachePartitionLostListenerConfig;
import com.hazelcast.config.CacheSimpleConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.CacheSimpleConfig.ExpiryPolicyFactoryConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.instance.BuildInfo;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.instance.impl.NodeExtension;
import com.hazelcast.internal.cluster.impl.ClusterServiceImpl;
import com.hazelcast.internal.dynamicconfig.DynamicConfigurationAwareConfig;
import com.hazelcast.internal.nio.Connection;
import com.hazelcast.internal.serialization.InternalSerializationService;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.security.SecurityInterceptorConstants;
import com.hazelcast.security.permission.ActionConstants;
import com.hazelcast.security.permission.UserCodeNamespacePermission;
import com.hazelcast.spi.impl.NodeEngine;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"checkstyle:NPathComplexity", "checkstyle:CyclomaticComplexity"})
public class AddCacheConfigMessageTask
        extends AbstractAddConfigMessageTask<DynamicConfigAddCacheConfigCodec.RequestParameters> {

    public AddCacheConfigMessageTask(ClientMessage clientMessage, Node node, Connection connection) {
        super(clientMessage, node, connection);
    }

    protected AddCacheConfigMessageTask(ClientMessage clientMessage, ILogger logger, NodeEngine nodeEngine,
            InternalSerializationService serializationService, ClientEngine clientEngine, Connection connection,
            NodeExtension nodeExtension, BuildInfo buildInfo, Config config, ClusterServiceImpl clusterService) {
        super(clientMessage, logger, nodeEngine, serializationService, clientEngine, connection, nodeExtension, buildInfo,
                config, clusterService);
    }

    @Override
    protected DynamicConfigAddCacheConfigCodec.RequestParameters decodeClientMessage(ClientMessage clientMessage) {
        return DynamicConfigAddCacheConfigCodec.decodeRequest(clientMessage);
    }

    @Override
    protected ClientMessage encodeResponse(Object response) {
        return DynamicConfigAddCacheConfigCodec.encodeResponse();
    }

    @Override
    protected IdentifiedDataSerializable getConfig() {
        CacheSimpleConfig config = new CacheSimpleConfig();
        config.setAsyncBackupCount(parameters.asyncBackupCount);
        config.setBackupCount(parameters.backupCount);
        config.setCacheEntryListeners(parameters.cacheEntryListeners);
        config.setCacheLoader(parameters.cacheLoader);
        config.setCacheLoaderFactory(parameters.cacheLoaderFactory);
        config.setCacheWriter(parameters.cacheWriter);
        config.setCacheWriterFactory(parameters.cacheWriterFactory);
        config.setDisablePerEntryInvalidationEvents(parameters.disablePerEntryInvalidationEvents);
        if (parameters.evictionConfig != null) {
            config.setEvictionConfig(parameters.evictionConfig.asEvictionConfig(serializationService));
        }
        if (parameters.expiryPolicyFactoryClassName != null) {
            config.setExpiryPolicyFactory(parameters.expiryPolicyFactoryClassName);
        } else if (parameters.timedExpiryPolicyFactoryConfig != null) {
            ExpiryPolicyFactoryConfig expiryPolicyFactoryConfig =
                    new ExpiryPolicyFactoryConfig(parameters.timedExpiryPolicyFactoryConfig);
            config.setExpiryPolicyFactoryConfig(expiryPolicyFactoryConfig);
        }
        if (parameters.eventJournalConfig != null) {
            config.setEventJournalConfig(parameters.eventJournalConfig);
        }
        if (parameters.hotRestartConfig != null) {
            config.setHotRestartConfig(parameters.hotRestartConfig);
        }
        config.setInMemoryFormat(InMemoryFormat.valueOf(parameters.inMemoryFormat));
        config.setKeyType(parameters.keyType);
        config.setManagementEnabled(parameters.managementEnabled);
        if (parameters.mergePolicy != null) {
            config.setMergePolicyConfig(mergePolicyConfig(parameters.mergePolicy, parameters.mergeBatchSize));
        }
        config.setName(parameters.name);
        if (parameters.partitionLostListenerConfigs != null && !parameters.partitionLostListenerConfigs.isEmpty()) {
            List<CachePartitionLostListenerConfig> listenerConfigs = (List<CachePartitionLostListenerConfig>)
                    adaptListenerConfigs(parameters.partitionLostListenerConfigs, parameters.userCodeNamespace);
            config.setPartitionLostListenerConfigs(listenerConfigs);
        } else {
            config.setPartitionLostListenerConfigs(new ArrayList<>());
        }
        config.setSplitBrainProtectionName(parameters.splitBrainProtectionName);
        config.setReadThrough(parameters.readThrough);
        config.setStatisticsEnabled(parameters.statisticsEnabled);
        config.setValueType(parameters.valueType);
        config.setWanReplicationRef(parameters.wanReplicationRef);
        config.setWriteThrough(parameters.writeThrough);
        if (parameters.isMerkleTreeConfigExists && parameters.merkleTreeConfig != null) {
            config.setMerkleTreeConfig(parameters.merkleTreeConfig);
        }
        if (parameters.isDataPersistenceConfigExists) {
            config.setDataPersistenceConfig(parameters.dataPersistenceConfig);
        }
        if (parameters.isUserCodeNamespaceExists) {
            config.setUserCodeNamespace(parameters.userCodeNamespace);
        }
        return config;
    }

    @Override
    public String getMethodName() {
        return SecurityInterceptorConstants.ADD_CACHE_CONFIG;
    }

    @Override
    public Permission getUserCodeNamespacePermission() {
        return parameters.userCodeNamespace != null
                ? new UserCodeNamespacePermission(parameters.userCodeNamespace, ActionConstants.ACTION_USE) : null;
    }

    @Override
    protected boolean checkStaticConfigDoesNotExist(IdentifiedDataSerializable config) {
        DynamicConfigurationAwareConfig nodeConfig = (DynamicConfigurationAwareConfig) nodeEngine.getConfig();
        CacheSimpleConfig cacheConfig = (CacheSimpleConfig) config;
        return DynamicConfigurationAwareConfig.checkStaticConfigDoesNotExist(nodeConfig.getStaticConfig().getCacheConfigs(),
                cacheConfig.getName(), cacheConfig);
    }
}
