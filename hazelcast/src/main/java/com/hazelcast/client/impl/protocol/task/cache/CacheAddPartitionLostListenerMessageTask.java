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

package com.hazelcast.client.impl.protocol.task.cache;

import com.hazelcast.cache.impl.CacheService;
import com.hazelcast.cache.impl.ICacheService;
import com.hazelcast.cache.impl.event.CachePartitionLostEventFilter;
import com.hazelcast.cache.impl.event.CachePartitionLostListener;
import com.hazelcast.cache.impl.event.InternalCachePartitionLostListenerAdapter;
import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.CacheAddPartitionLostListenerCodec;
import com.hazelcast.client.impl.protocol.task.AbstractAddListenerMessageTask;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.nio.Connection;
import com.hazelcast.security.permission.ActionConstants;
import com.hazelcast.security.permission.CachePermission;
import com.hazelcast.security.permission.NamespacePermission;
import com.hazelcast.security.SecurityInterceptorConstants;
import com.hazelcast.spi.impl.eventservice.EventFilter;
import com.hazelcast.spi.impl.eventservice.EventRegistration;
import com.hazelcast.spi.impl.eventservice.EventService;

import java.security.Permission;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.hazelcast.internal.util.ConcurrencyUtil.CALLER_RUNS;
import static com.hazelcast.spi.impl.InternalCompletableFuture.newCompletedFuture;

public class CacheAddPartitionLostListenerMessageTask
        extends AbstractAddListenerMessageTask<CacheAddPartitionLostListenerCodec.RequestParameters> {

    public CacheAddPartitionLostListenerMessageTask(ClientMessage clientMessage, Node node, Connection connection) {
        super(clientMessage, node, connection);
    }

    @Override
    protected CompletableFuture<UUID> processInternal() {
        CachePartitionLostListener listener = event -> {
            if (endpoint.isAlive()) {
                ClientMessage eventMessage = CacheAddPartitionLostListenerCodec
                        .encodeCachePartitionLostEvent(event.getPartitionId(), event.getMember().getUuid());
                sendClientMessage(null, eventMessage);
            }
        };

        InternalCachePartitionLostListenerAdapter listenerAdapter =
                new InternalCachePartitionLostListenerAdapter(listener);
        EventFilter filter = new CachePartitionLostEventFilter();
        CacheService service = getService(getServiceName());
        EventService eventService = service.getNodeEngine().getEventService();
        if (parameters.localOnly) {
            return newCompletedFuture(
                    eventService.registerLocalListener(getServiceName(), parameters.name, filter, listenerAdapter)
                                .getId());
        }

        return eventService.registerListenerAsync(getServiceName(), parameters.name, filter, listenerAdapter)
                           .thenApplyAsync(EventRegistration::getId, CALLER_RUNS);
    }

    @Override
    protected CacheAddPartitionLostListenerCodec.RequestParameters decodeClientMessage(ClientMessage clientMessage) {
        return CacheAddPartitionLostListenerCodec.decodeRequest(clientMessage);
    }

    @Override
    protected ClientMessage encodeResponse(Object response) {
        return CacheAddPartitionLostListenerCodec.encodeResponse((UUID) response);
    }

    @Override
    public String getServiceName() {
        return ICacheService.SERVICE_NAME;
    }

    @Override
    public String getMethodName() {
        return SecurityInterceptorConstants.ADD_PARTITION_LOST_LISTENER;
    }

    @Override
    public Object[] getParameters() {
        return null;
    }

    @Override
    public Permission getRequiredPermission() {
        return null;
    }

    @Override
    public Collection<Permission> getRequiredPermissions() {
        Collection<Permission> permissions = new HashSet<>();
        permissions.add(new CachePermission(getDistributedObjectName(), ActionConstants.ACTION_LISTEN));

        CacheService service = getService(getServiceName());
        String namespace = service.getNamespace(getDistributedObjectName());

        if (namespace != null) {
            permissions.add(new NamespacePermission(namespace, ActionConstants.ACTION_USE));
        }

        return permissions;
    }

    @Override
    public String getDistributedObjectName() {
        return parameters.name;
    }

    @Override
    protected String getNamespace() {
        final CacheService service = getService(CacheService.SERVICE_NAME);
        return service.getNamespace(parameters.name);
    }
}
