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

package com.hazelcast.client.impl.protocol.task.executorservice;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.ExecutorServiceCancelOnPartitionCodec;
import com.hazelcast.client.impl.protocol.task.AbstractPartitionMessageTask;
import com.hazelcast.executor.impl.DistributedExecutorService;
import com.hazelcast.executor.impl.operations.CancellationOperation;
import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.nio.Connection;
import com.hazelcast.security.SecurityInterceptorConstants;
import com.hazelcast.security.permission.ActionConstants;
import com.hazelcast.security.permission.ExecutorServicePermission;
import com.hazelcast.spi.impl.operationservice.Operation;

import java.security.Permission;

public class ExecutorServiceCancelOnPartitionMessageTask
        extends AbstractPartitionMessageTask<ExecutorServiceCancelOnPartitionCodec.RequestParameters> {

    public ExecutorServiceCancelOnPartitionMessageTask(ClientMessage clientMessage, Node node, Connection connection) {
        super(clientMessage, node, connection);
    }

    @Override
    protected Operation prepareOperation() {
        return new CancellationOperation(parameters.uuid, parameters.interrupt);
    }

    @Override
    protected ExecutorServiceCancelOnPartitionCodec.RequestParameters decodeClientMessage(ClientMessage clientMessage) {
        return ExecutorServiceCancelOnPartitionCodec.decodeRequest(clientMessage);
    }

    protected ClientMessage encodeResponse(Object response) {
        return ExecutorServiceCancelOnPartitionCodec.encodeResponse((Boolean) response);
    }

    @Override
    public String getDistributedObjectName() {
        DistributedExecutorService service = getService(getServiceName());
        return service.getName(parameters.uuid);
    }

    @Override
    public String getServiceName() {
        return DistributedExecutorService.SERVICE_NAME;
    }

    @Override
    public Permission getRequiredPermission() {
        String name = getDistributedObjectName();
        if (name == null) {
            // The permission constructor expects a non-null name.
            name = ExecutorServicePermission.EMPTY_EXECUTOR_NAME;
        }

        return new ExecutorServicePermission(name, ActionConstants.ACTION_MODIFY);
    }

    @Override
    public String getMethodName() {
        return SecurityInterceptorConstants.CANCEL;
    }

    @Override
    public Object[] getParameters() {
        return null;
    }

    @Override
    protected String getUserCodeNamespace() {
        // This task is not Namespace-aware so it doesn't matter
        return null;
    }
}
