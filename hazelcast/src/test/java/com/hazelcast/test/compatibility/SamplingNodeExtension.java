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

package com.hazelcast.test.compatibility;

import com.hazelcast.auditlog.AuditlogService;
import com.hazelcast.auditlog.impl.NoOpAuditlogService;
import com.hazelcast.client.impl.ClientEngine;
import com.hazelcast.cluster.ClusterState;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.cp.internal.persistence.CPPersistenceService;
import com.hazelcast.hotrestart.HotRestartService;
import com.hazelcast.instance.EndpointQualifier;
import com.hazelcast.instance.impl.HazelcastInstanceImpl;
import com.hazelcast.instance.impl.NodeExtension;
import com.hazelcast.internal.ascii.TextCommandService;
import com.hazelcast.internal.cluster.impl.JoinMessage;
import com.hazelcast.internal.diagnostics.Diagnostics;
import com.hazelcast.internal.diagnostics.HealthMonitor;
import com.hazelcast.internal.hotrestart.InternalHotRestartService;
import com.hazelcast.internal.jmx.ManagementService;
import com.hazelcast.internal.management.TimedMemberStateFactory;
import com.hazelcast.internal.memory.MemoryStats;
import com.hazelcast.internal.namespace.UserCodeNamespaceService;
import com.hazelcast.internal.networking.ChannelInitializer;
import com.hazelcast.internal.networking.InboundHandler;
import com.hazelcast.internal.networking.OutboundHandler;
import com.hazelcast.internal.serialization.InternalSerializationService;
import com.hazelcast.internal.serialization.SerializationService;
import com.hazelcast.internal.serialization.impl.AbstractSerializationService;
import com.hazelcast.internal.serialization.impl.compact.schema.MemberSchemaService;
import com.hazelcast.internal.server.ServerConnection;
import com.hazelcast.internal.server.ServerContext;
import com.hazelcast.internal.tpc.TpcServerBootstrap;
import com.hazelcast.internal.tpc.TpcServerBootstrapImpl;
import com.hazelcast.internal.util.ByteArrayProcessor;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.impl.JetServiceBackend;
import com.hazelcast.nio.MemberSocketInterceptor;
import com.hazelcast.nio.ssl.SSLEngineFactory;
import com.hazelcast.security.SecurityContext;
import com.hazelcast.security.SecurityService;
import com.hazelcast.spi.impl.NodeEngine;
import com.hazelcast.version.Version;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Node extension that instantiates a {@link SamplingSerializationService} when asked to create
 * {@link SerializationService} instance.
 */
public class SamplingNodeExtension implements NodeExtension {

    private final NodeExtension nodeExtension;

    public SamplingNodeExtension(NodeExtension nodeExtension) {
        this.nodeExtension = nodeExtension;
    }

    @Override
    public InternalSerializationService createSerializationService() {
        InternalSerializationService serializationService = nodeExtension.createSerializationService();
        return new SamplingSerializationService((AbstractSerializationService) serializationService);
    }

    @Override
    public InternalSerializationService createCompatibilitySerializationService() {
        InternalSerializationService serializationService = nodeExtension.createCompatibilitySerializationService();
        return new SamplingSerializationService((AbstractSerializationService) serializationService);
    }

    @Override
    public MemberSchemaService createSchemaService() {
        return nodeExtension.createSchemaService();
    }

    @Override
    public SecurityService getSecurityService() {
        return nodeExtension.getSecurityService();
    }

    @Override
    public void beforeStart() {
        nodeExtension.beforeStart();
    }

    @Override
    public void printNodeInfo() {
        nodeExtension.printNodeInfo();
    }

    @Override
    public void logInstanceTrackingMetadata() {
        nodeExtension.logInstanceTrackingMetadata();
    }

    @Override
    public void beforeJoin() {
        nodeExtension.beforeJoin();
    }

    @Override
    public void afterStart() {
        nodeExtension.afterStart();
    }

    @Override
    public boolean isStartCompleted() {
        return nodeExtension.isStartCompleted();
    }

    @Override
    public boolean isReady() {
        return nodeExtension.isReady();
    }

    @Override
    public void beforeShutdown(boolean terminate) {
        nodeExtension.beforeShutdown(terminate);
    }

    @Override
    public void shutdown() {
        nodeExtension.shutdown();
    }

    @Override
    public void afterShutdown() {
        nodeExtension.afterShutdown();
    }

    @Override
    public SecurityContext getSecurityContext() {
        return nodeExtension.getSecurityContext();
    }

    @Override
    public <T> T createService(Class<T> type, Object... params) {
        return nodeExtension.createService(type, params);
    }

    @Override
    public Map<String, Object> createExtensionServices() {
        return nodeExtension.createExtensionServices();
    }

    @Override
    public MemberSocketInterceptor getSocketInterceptor(EndpointQualifier endpointQualifier) {
        return nodeExtension.getSocketInterceptor(endpointQualifier);
    }

    @Override
    public InboundHandler[] createInboundHandlers(EndpointQualifier qualifier, ServerConnection connection, ServerContext serverContext) {
        return nodeExtension.createInboundHandlers(qualifier, connection, serverContext);
    }

    @Override
    public OutboundHandler[] createOutboundHandlers(EndpointQualifier qualifier, ServerConnection connection, ServerContext serverContext) {
        return nodeExtension.createOutboundHandlers(qualifier, connection, serverContext);
    }

    @Override
    public Function<EndpointQualifier, ChannelInitializer> createChannelInitializerFn(ServerContext serverContext) {
       return nodeExtension.createChannelInitializerFn(serverContext);
    }

    @Override
    public void onThreadStart(Thread thread) {
        nodeExtension.onThreadStart(thread);
    }

    @Override
    public void onThreadStop(Thread thread) {
        nodeExtension.onThreadStop(thread);
    }

    @Override
    public MemoryStats getMemoryStats() {
        return nodeExtension.getMemoryStats();
    }

    @Override
    public void validateJoinRequest(JoinMessage joinMessage) {
        nodeExtension.validateJoinRequest(joinMessage);
    }

    @Override
    public Set<Version> getSupportedVersions() {
        return nodeExtension.getSupportedVersions();
    }

    @Override
    public void beforeClusterStateChange(ClusterState currState, ClusterState requestedState, boolean isTransient) {
        nodeExtension.beforeClusterStateChange(currState, requestedState, isTransient);
    }

    @Override
    public void onInitialClusterState(ClusterState initialState) {
        nodeExtension.onInitialClusterState(initialState);
    }

    @Override
    public void onClusterStateChange(ClusterState newState, boolean isTransient) {
        nodeExtension.onClusterStateChange(newState, isTransient);
    }

    @Override
    public void afterClusterStateChange(ClusterState oldState, ClusterState newState, boolean isTransient) {
        nodeExtension.afterClusterStateChange(oldState, newState, isTransient);
    }

    @Override
    public void onPartitionStateChange() {
        nodeExtension.onPartitionStateChange();
    }

    @Override
    public void onMemberListChange() {
        nodeExtension.onMemberListChange();
    }

    @Override
    public void onClusterVersionChange(Version newVersion) {
        nodeExtension.onClusterVersionChange(newVersion);
    }

    @Override
    public boolean isNodeVersionCompatibleWith(Version clusterVersion) {
        return nodeExtension.isNodeVersionCompatibleWith(clusterVersion);
    }

    @Override
    public boolean registerListener(Object listener) {
        return nodeExtension.registerListener(listener);
    }

    @Override
    public HotRestartService getHotRestartService() {
        return nodeExtension.getHotRestartService();
    }

    @Override
    public InternalHotRestartService getInternalHotRestartService() {
        return nodeExtension.getInternalHotRestartService();
    }

    @Override
    public UUID createMemberUuid() {
        return nodeExtension.createMemberUuid();
    }

    @Override
    public TimedMemberStateFactory createTimedMemberStateFactory(HazelcastInstanceImpl instance) {
        return nodeExtension.createTimedMemberStateFactory(instance);
    }

    @Override
    public ByteArrayProcessor createMulticastInputProcessor(ServerContext serverContext) {
        return nodeExtension.createMulticastInputProcessor(serverContext);
    }

    @Override
    public ByteArrayProcessor createMulticastOutputProcessor(ServerContext serverContext) {
        return nodeExtension.createMulticastOutputProcessor(serverContext);
    }

    @Override
    public void registerPlugins(Diagnostics diagnostics) {
    }

    @Override
    public ManagementService createJMXManagementService(HazelcastInstanceImpl instance) {
        return nodeExtension.createJMXManagementService(instance);
    }

    @Override
    public TextCommandService createTextCommandService() {
        return nodeExtension.createTextCommandService();
    }

    @Override
    public void scheduleClusterVersionAutoUpgrade() {
        nodeExtension.scheduleClusterVersionAutoUpgrade();
    }

    @Override
    public boolean isClientFailoverSupported() {
        return false;
    }

    @Override
    public AuditlogService getAuditlogService() {
        return NoOpAuditlogService.INSTANCE;
    }

    @Override
    public CPPersistenceService getCPPersistenceService() {
        return nodeExtension.getCPPersistenceService();
    }

    @Override
    public CPSubsystem createCPSubsystem(NodeEngine nodeEngine) {
        return nodeExtension.createCPSubsystem(nodeEngine);
    }

    @Override
    public JetService getJet() {
        return nodeExtension.getJet();
    }

    @Nullable
    @Override
    public JetServiceBackend getJetServiceBackend() {
        return nodeExtension.getJetServiceBackend();
    }

    @Override
    public SSLEngineFactory createSslEngineFactory(SSLConfig sslConfig) {
        return nodeExtension.createSslEngineFactory(sslConfig);
    }

    @Override
    public UserCodeNamespaceService getNamespaceService() {
        return nodeExtension.getNamespaceService();
    }

    @Override
    public TpcServerBootstrap createTpcServerBootstrap() {
        return new TpcServerBootstrapImpl(null);
    }

    @Override
    public ClientEngine createClientEngine() {
        return nodeExtension.createClientEngine();
    }

    @Nullable
    @Override
    public Object getLicense() {
        return nodeExtension.getLicense();
    }

    @Override
    public HealthMonitor createHealthMonitor() {
        return nodeExtension.createHealthMonitor();
    }
}
