/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.client.impl.protocol.task.cp;

import com.hazelcast.client.impl.CPGroupViewListenerService;
import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.ClientAddCPGroupViewListenerCodec;
import com.hazelcast.client.impl.protocol.task.AbstractCallableMessageTask;
import com.hazelcast.cp.internal.RaftServiceUtil;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.nio.Connection;
import com.hazelcast.internal.util.UuidUtil;

import java.security.Permission;

public class AddCPGroupViewListenerMessageTask extends AbstractCallableMessageTask<Void> {

    public AddCPGroupViewListenerMessageTask(ClientMessage clientMessage, Node node, Connection connection) {
        super(clientMessage, node, connection);
    }

    @Override
    protected boolean acceptOnIncompleteStart() {
        return true;
    }

    @Override
    protected Object call() {
        CPGroupViewListenerService service = clientEngine.getCPGroupViewListenerService();
        service.registerListener(endpoint, clientMessage.getCorrelationId());
        endpoint.addDestroyAction(UuidUtil.newUnsecureUUID(), () -> {
            service.deregisterListener(endpoint);
            return Boolean.TRUE;
        });

        return true;
    }

    @Override
    protected Void decodeClientMessage(ClientMessage clientMessage) {
        return null;
    }

    @Override
    protected ClientMessage encodeResponse(Object response) {
        return ClientAddCPGroupViewListenerCodec.encodeResponse();
    }

    @Override
    public String getServiceName() {
        return RaftServiceUtil.SERVICE_NAME;
    }

    @Override
    public String getDistributedObjectName() {
        return null;
    }

    @Override
    public String getMethodName() {
        return null;
    }

    @Override
    public Object[] getParameters() {
        return null;
    }

    public Permission getRequiredPermission() {
        return null;
    }

}
