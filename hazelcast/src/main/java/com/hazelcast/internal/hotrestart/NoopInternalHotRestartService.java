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

package com.hazelcast.internal.hotrestart;

import com.hazelcast.cluster.ClusterState;
import com.hazelcast.cluster.Member;
import com.hazelcast.instance.impl.ClusterTopologyIntent;
import com.hazelcast.internal.cluster.impl.operations.OnJoinOp;
import com.hazelcast.internal.management.dto.ClusterHotRestartStatusDTO;
import com.hazelcast.cluster.Address;
import com.hazelcast.internal.partition.PartitionRuntimeState;
import com.hazelcast.internal.partition.operation.SafeStateCheckOperation;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Empty implementation of InternalHotRestartService to avoid null checks. This will provide default behaviour when hot restart
 * is not available or not enabled.
 */
public class NoopInternalHotRestartService implements InternalHotRestartService {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isStartCompleted() {
        return true;
    }

    @Override
    public boolean triggerForceStart() {
        return false;
    }

    @Override
    public boolean triggerPartialStart() {
        return false;
    }

    @Override
    public boolean isMemberExcluded(Address memberAddress, UUID memberUuid) {
        return false;
    }

    @Override
    public Set<UUID> getExcludedMemberUuids() {
        return Collections.emptySet();
    }

    @Override
    public void notifyExcludedMember(Address memberAddress) {
    }

    @Override
    public void handleExcludedMemberUuids(Address sender, Set<UUID> excludedMemberUuids) {
    }

    @Override
    public ClusterHotRestartStatusDTO getCurrentClusterHotRestartStatus() {
        return new ClusterHotRestartStatusDTO();
    }

    @Override
    public void resetService(boolean isAfterJoin) {
    }

    @Override
    public void forceStartBeforeJoin() {
    }

    @Override
    public void waitPartitionReplicaSyncOnCluster(long timeout, TimeUnit unit,
                                                  Supplier<SafeStateCheckOperation> supplier) {
    }

    @Override
    public void setRejoiningActiveCluster(boolean rejoiningActiveCluster) {
    }

    @Override
    public void deferApplyPartitionState(PartitionRuntimeState partitionRuntimeState) {
    }

    @Override
    public void deferPostJoinOps(OnJoinOp postJoinOp) {
    }

    @Override
    public void setClusterTopologyIntentOnMaster(ClusterTopologyIntent clusterTopologyIntent) {
    }

    @Override
    public boolean isClusterMetadataFoundOnDisk() {
        return false;
    }

    @Override
    public void onClusterTopologyIntentChange() {
    }

    @Override
    public boolean trySetDeferredClusterState(ClusterState newClusterState) {
        return false;
    }

    @Override
    public void onMemberLeft(Member member, ClusterState clusterState) {
    }

    @Override
    public void onMemberJoined(Member member) {
    }
}
