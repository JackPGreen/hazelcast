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

package com.hazelcast.instance.impl;

import com.hazelcast.internal.cluster.Joiner;
import com.hazelcast.instance.AddressPicker;
import com.hazelcast.internal.server.tcp.LocalAddressRegistry;
import com.hazelcast.internal.server.tcp.ServerSocketRegistry;
import com.hazelcast.internal.server.Server;

/**
 * A context for node to provide its dependencies. Acts as a dependency factory.
 * <p>
 * Normally, there is a default context. But to be able to make tests simpler,
 * to run them faster and in-parallel, it's necessary to avoid network and some heavy-weight
 * objects creations. That's why most of the tests use a special purpose NodeContext.
 */
public interface NodeContext {

    NodeExtension createNodeExtension(Node node);

    AddressPicker createAddressPicker(Node node);

    Joiner createJoiner(Node node);

    Server createServer(Node node, ServerSocketRegistry registry, LocalAddressRegistry addressRegistry);
}
