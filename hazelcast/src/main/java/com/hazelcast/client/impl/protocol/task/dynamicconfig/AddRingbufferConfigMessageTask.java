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
import com.hazelcast.client.impl.protocol.codec.DynamicConfigAddRingbufferConfigCodec;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MergePolicyConfig;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.config.RingbufferStoreConfig;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.dynamicconfig.DynamicConfigurationAwareConfig;
import com.hazelcast.internal.nio.Connection;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.security.SecurityInterceptorConstants;
import com.hazelcast.security.permission.ActionConstants;
import com.hazelcast.security.permission.NamespacePermission;

import java.security.Permission;
import java.util.Collection;

public class AddRingbufferConfigMessageTask
        extends AbstractAddConfigMessageTask<DynamicConfigAddRingbufferConfigCodec.RequestParameters> {

    public AddRingbufferConfigMessageTask(ClientMessage clientMessage, Node node, Connection connection) {
        super(clientMessage, node, connection);
    }

    @Override
    protected DynamicConfigAddRingbufferConfigCodec.RequestParameters decodeClientMessage(ClientMessage clientMessage) {
        return DynamicConfigAddRingbufferConfigCodec.decodeRequest(clientMessage);
    }

    @Override
    protected ClientMessage encodeResponse(Object response) {
        return DynamicConfigAddRingbufferConfigCodec.encodeResponse();
    }

    @Override
    protected IdentifiedDataSerializable getConfig() {
        RingbufferConfig config = new RingbufferConfig(parameters.name);
        config.setAsyncBackupCount(parameters.asyncBackupCount);
        config.setBackupCount(parameters.backupCount);
        config.setCapacity(parameters.capacity);
        config.setInMemoryFormat(InMemoryFormat.valueOf(parameters.inMemoryFormat));
        config.setTimeToLiveSeconds(parameters.timeToLiveSeconds);
        if (parameters.ringbufferStoreConfig != null) {
            RingbufferStoreConfig storeConfig = parameters.ringbufferStoreConfig.asRingbufferStoreConfig(serializationService);
            config.setRingbufferStoreConfig(storeConfig);
        }
        MergePolicyConfig mergePolicyConfig = mergePolicyConfig(parameters.mergePolicy, parameters.mergeBatchSize);
        config.setMergePolicyConfig(mergePolicyConfig);
        if (parameters.isNamespaceExists) {
            config.setNamespace(parameters.namespace);
        }
        return config;
    }

    @Override
    public String getMethodName() {
        return SecurityInterceptorConstants.ADD_RINGBUFFER_CONFIG;
    }

    @Override
    public Collection<Permission> getRequiredPermissions() {
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
        RingbufferConfig ringbufferConfig = (RingbufferConfig) config;
        return DynamicConfigurationAwareConfig.checkStaticConfigDoesNotExist(nodeConfig.getStaticConfig().getRingbufferConfigs(),
                ringbufferConfig.getName(), ringbufferConfig);
    }
}
