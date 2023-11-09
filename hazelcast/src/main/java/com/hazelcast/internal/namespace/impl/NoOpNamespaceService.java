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

package com.hazelcast.internal.namespace.impl;

import com.hazelcast.internal.namespace.NamespaceService;
import com.hazelcast.internal.namespace.ResourceDefinition;
import com.hazelcast.internal.util.ExceptionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.Callable;

public final class NoOpNamespaceService implements NamespaceService {
    private final ClassLoader configClassLoader;

    public NoOpNamespaceService(ClassLoader configClassLoader) {
        this.configClassLoader = configClassLoader;
    }

    @Override
    public void addNamespace(@Nonnull String nsName, @Nonnull Collection<ResourceDefinition> resources) {
        // No-op
    }

    @Override
    public boolean removeNamespace(@Nonnull String nsName) {
        return false;
    }

    @Override
    public boolean hasNamespace(String namespaceName) {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isDefaultNamespaceDefined() {
        return false;
    }

    @Override
    public void setupNamespace(@Nullable String namespace) {
        // No-op
    }

    @Override
    public void cleanupNamespace(@Nullable String namespace) {
        // No-op
    }

    @Override
    public void runWithNamespace(@Nullable String namespace, Runnable runnable) {
        runnable.run();
    }

    @Override
    public <V> V callWithNamespace(@Nullable String namespace, Callable<V> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw ExceptionUtil.sneakyThrow(e);
        }
    }

    @Override
    public ClassLoader getClassLoaderForNamespace(String namespace) {
        return configClassLoader;
    }
}
