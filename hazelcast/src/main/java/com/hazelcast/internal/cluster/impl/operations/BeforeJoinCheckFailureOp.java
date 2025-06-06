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

package com.hazelcast.internal.cluster.impl.operations;

import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.cluster.impl.ClusterDataSerializerHook;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.spi.impl.NodeEngine;

import java.io.IOException;

public class BeforeJoinCheckFailureOp extends AbstractClusterOperation {

    private String failReasonMsg;

    public BeforeJoinCheckFailureOp() {
    }

    public BeforeJoinCheckFailureOp(String failReasonMsg) {
        this.failReasonMsg = failReasonMsg;
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        out.writeString(failReasonMsg);
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        failReasonMsg = in.readString();
    }

    @Override
    public void run() {
        final NodeEngine nodeEngine = getNodeEngine();
        final Node node = nodeEngine.getNode();
        JoinOperation.verifyCanShutdown(node, failReasonMsg);

        final ILogger logger = nodeEngine.getLogger("com.hazelcast.security");
        logger.severe("Node could not join cluster. Before join check failed node is going to shutdown now!");
        logger.severe("Reason of failure for node join: " + failReasonMsg);
        node.shutdown(true);
    }

    @Override
    public int getClassId() {
        return ClusterDataSerializerHook.BEFORE_JOIN_CHECK_FAILURE;
    }
}
