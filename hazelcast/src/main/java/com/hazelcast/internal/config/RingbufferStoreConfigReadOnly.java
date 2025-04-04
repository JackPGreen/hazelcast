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

import com.hazelcast.config.RingbufferStoreConfig;
import com.hazelcast.ringbuffer.RingbufferStore;
import com.hazelcast.ringbuffer.RingbufferStoreFactory;

import javax.annotation.Nonnull;
import java.util.Properties;

public class RingbufferStoreConfigReadOnly extends RingbufferStoreConfig {

    public RingbufferStoreConfigReadOnly(RingbufferStoreConfig config) {
        super(config);
    }

    @Override
    public RingbufferStoreConfig setStoreImplementation(@Nonnull RingbufferStore storeImplementation) {
        throw new UnsupportedOperationException("This config is read-only.");
    }

    @Override
    public RingbufferStoreConfig setEnabled(boolean enabled) {
        throw new UnsupportedOperationException("This config is read-only.");
    }

    @Override
    public RingbufferStoreConfig setClassName(@Nonnull String className) {
        throw new UnsupportedOperationException("This config is read-only.");
    }

    @Override
    public RingbufferStoreConfig setProperties(Properties properties) {
        throw new UnsupportedOperationException("This config is read-only.");
    }

    @Override
    public RingbufferStoreConfig setProperty(String name, String value) {
        throw new UnsupportedOperationException("This config is read-only.");
    }

    @Override
    public RingbufferStoreConfig setFactoryClassName(@Nonnull String factoryClassName) {
        throw new UnsupportedOperationException("This config is read-only.");
    }

    @Override
    public RingbufferStoreConfig setFactoryImplementation(@Nonnull RingbufferStoreFactory factoryImplementation) {
        throw new UnsupportedOperationException("This config is read-only.");
    }
}
