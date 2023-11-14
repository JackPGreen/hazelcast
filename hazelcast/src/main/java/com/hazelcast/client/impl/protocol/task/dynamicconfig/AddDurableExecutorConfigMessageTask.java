/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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
import com.hazelcast.client.impl.protocol.codec.DynamicConfigAddDurableExecutorConfigCodec;
import com.hazelcast.config.DurableExecutorConfig;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.dynamicconfig.DynamicConfigurationAwareConfig;
import com.hazelcast.internal.nio.Connection;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.security.SecurityInterceptorConstants;
import com.hazelcast.security.permission.ActionConstants;
import com.hazelcast.security.permission.NamespacePermission;

import java.security.Permission;

public class AddDurableExecutorConfigMessageTask
        extends AbstractAddConfigMessageTask<DynamicConfigAddDurableExecutorConfigCodec.RequestParameters> {

    public AddDurableExecutorConfigMessageTask(ClientMessage clientMessage, Node node, Connection connection) {
        super(clientMessage, node, connection);
    }

    @Override
    protected DynamicConfigAddDurableExecutorConfigCodec.RequestParameters decodeClientMessage(ClientMessage clientMessage) {
        return DynamicConfigAddDurableExecutorConfigCodec.decodeRequest(clientMessage);
    }

    @Override
    protected ClientMessage encodeResponse(Object response) {
        return DynamicConfigAddDurableExecutorConfigCodec.encodeResponse();
    }

    @Override
    protected IdentifiedDataSerializable getConfig() {
        // This is to handle 4.0 client versions. Those
        // versions don't aware of `statisticsEnabled` parameter.
        // The parameter was added at version 4.1 and its default value is  true.
        boolean statsEnabled = !parameters.isStatisticsEnabledExists
                || parameters.statisticsEnabled;

        DurableExecutorConfig config = new DurableExecutorConfig(parameters.name, parameters.poolSize,
                parameters.durability, parameters.capacity, statsEnabled, parameters.namespace);
        return config;
    }

    @Override
    public String getMethodName() {
        return SecurityInterceptorConstants.ADD_DURABLE_EXECUTOR_CONFIG;
    }

    @Override
    public Permission[] getRequiredPermissions() {
        if (parameters.namespace == null) {
            return super.getRequiredPermissions();
        } else {
            // Require NamespacePermissions as the config is namespace aware - e.g. if inflating a MapStore, could be required
            return extendPermissions(super.getRequiredPermissions(),
                    new NamespacePermission(parameters.namespace, ActionConstants.ACTION_USE));
        }
    }

    @Override
    protected boolean checkStaticConfigDoesNotExist(IdentifiedDataSerializable config) {
        DynamicConfigurationAwareConfig nodeConfig = (DynamicConfigurationAwareConfig) nodeEngine.getConfig();
        DurableExecutorConfig durableExecutorConfig = (DurableExecutorConfig) config;
        return DynamicConfigurationAwareConfig.checkStaticConfigDoesNotExist(nodeConfig.getStaticConfig().getDurableExecutorConfigs(),
                durableExecutorConfig.getName(), durableExecutorConfig);
    }
}
