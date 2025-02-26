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
package com.hazelcast.client.impl.clientside;

import com.hazelcast.client.impl.connection.ClientConnectionManager;
import com.hazelcast.client.impl.spi.ClientClusterService;
import com.hazelcast.client.impl.spi.ClientInvocationService;
import com.hazelcast.client.impl.spi.ClientListenerService;
import com.hazelcast.client.impl.spi.impl.listener.ClientCPGroupViewService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.impl.executionservice.TaskScheduler;

public interface HazelcastClientInstance extends HazelcastInstance {
    /**
     * Get connection manager
     */
    ClientConnectionManager getConnectionManager();

    /**
     * Get task scheduler
     */
    TaskScheduler getTaskScheduler();

    /**
     * Get invocation service
     */
    ClientInvocationService getInvocationService();

    /**
     * Get listener service
     */
    ClientListenerService getListenerService();

    /**
     * Get cluster service
     */
    ClientClusterService getClientClusterService();

    /**
     * Get CP group view service
     */
    ClientCPGroupViewService getCPGroupViewService();
}
