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

package com.hazelcast.internal.config;

import com.hazelcast.config.UserCodeNamespaceConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;

public class UserCodeNamespaceConfigReadOnly
        extends UserCodeNamespaceConfig {

    public UserCodeNamespaceConfigReadOnly(UserCodeNamespaceConfig config) {
        super(config);
    }

    @Override
    public UserCodeNamespaceConfig setName(String name) {
        throw new UnsupportedOperationException("This config is read-only namespace: " + getName());
    }


    @Override
    public UserCodeNamespaceConfig addClass(@Nonnull Class<?>... classes) {
        throw new UnsupportedOperationException("This config is read-only namespace: " + getName());
    }

    @Override
    public UserCodeNamespaceConfig addClass(@Nonnull URL url, @Nullable String id) {
        throw new UnsupportedOperationException("This config is read-only namespace: " + getName());
    }

    @Override
    public UserCodeNamespaceConfig addJar(@Nonnull URL url, @Nullable String id) {
        throw new UnsupportedOperationException("This config is read-only namespace: " + getName());
    }

    @Override
    public UserCodeNamespaceConfig addJarsInZip(@Nonnull URL url, @Nullable String id) {
        throw new UnsupportedOperationException("This config is read-only namespace: " + getName());
    }
}
