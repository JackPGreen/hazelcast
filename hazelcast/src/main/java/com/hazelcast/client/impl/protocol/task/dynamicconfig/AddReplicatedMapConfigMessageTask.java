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

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.DynamicConfigAddReplicatedMapConfigCodec;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MergePolicyConfig;
import com.hazelcast.config.ReplicatedMapConfig;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.dynamicconfig.DynamicConfigurationAwareConfig;
import com.hazelcast.internal.nio.Connection;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.security.SecurityInterceptorConstants;
import com.hazelcast.security.permission.ActionConstants;
import com.hazelcast.security.permission.UserCodeNamespacePermission;

import java.security.Permission;
import java.util.ArrayList;

public class AddReplicatedMapConfigMessageTask
        extends AbstractAddConfigMessageTask<DynamicConfigAddReplicatedMapConfigCodec.RequestParameters> {

    public AddReplicatedMapConfigMessageTask(ClientMessage clientMessage, Node node, Connection connection) {
        super(clientMessage, node, connection);
    }

    @Override
    protected DynamicConfigAddReplicatedMapConfigCodec.RequestParameters decodeClientMessage(ClientMessage clientMessage) {
        return DynamicConfigAddReplicatedMapConfigCodec.decodeRequest(clientMessage);
    }

    @Override
    protected ClientMessage encodeResponse(Object response) {
        return DynamicConfigAddReplicatedMapConfigCodec.encodeResponse();
    }

    @Override
    protected IdentifiedDataSerializable getConfig() {
        ReplicatedMapConfig config = new ReplicatedMapConfig(parameters.name);
        config.setAsyncFillup(parameters.asyncFillup);
        config.setInMemoryFormat(InMemoryFormat.valueOf(parameters.inMemoryFormat));
        MergePolicyConfig mergePolicyConfig = mergePolicyConfig(parameters.mergePolicy, parameters.mergeBatchSize);
        config.setMergePolicyConfig(mergePolicyConfig);
        config.setStatisticsEnabled(parameters.statisticsEnabled);
        if (parameters.listenerConfigs != null && !parameters.listenerConfigs.isEmpty()) {
            for (ListenerConfigHolder holder : parameters.listenerConfigs) {
                config.addEntryListenerConfig(holder.asListenerConfig(serializationService, parameters.userCodeNamespace));
            }
        } else {
            config.setListenerConfigs(new ArrayList<>());
        }
        if (parameters.isUserCodeNamespaceExists) {
            config.setUserCodeNamespace(parameters.userCodeNamespace);
        }
        return config;
    }

    @Override
    public String getMethodName() {
        return SecurityInterceptorConstants.ADD_REPLICATED_MAP_CONFIG;
    }

    @Override
    public Permission getUserCodeNamespacePermission() {
        return parameters.userCodeNamespace != null
                ? new UserCodeNamespacePermission(parameters.userCodeNamespace, ActionConstants.ACTION_USE) : null;
    }

    @Override
    protected boolean checkStaticConfigDoesNotExist(IdentifiedDataSerializable config) {
        DynamicConfigurationAwareConfig nodeConfig = (DynamicConfigurationAwareConfig) nodeEngine.getConfig();
        ReplicatedMapConfig replicatedMapConfig = (ReplicatedMapConfig) config;
        return DynamicConfigurationAwareConfig.checkStaticConfigDoesNotExist(
                nodeConfig.getStaticConfig().getReplicatedMapConfigs(), replicatedMapConfig.getName(), replicatedMapConfig);
    }
}
