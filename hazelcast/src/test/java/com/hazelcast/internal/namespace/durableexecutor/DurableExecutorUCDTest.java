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

package com.hazelcast.internal.namespace.durableexecutor;

import com.hazelcast.config.Config;
import com.hazelcast.config.DurableExecutorConfig;
import com.hazelcast.durableexecutor.DurableExecutorService;
import com.hazelcast.internal.namespace.UCDTest;

public abstract class DurableExecutorUCDTest extends UCDTest {
    protected DurableExecutorConfig durableExecutorConfig;
    protected DurableExecutorService executor;

    @Override
    public void setUpInstance() throws ReflectiveOperationException {
        durableExecutorConfig = new DurableExecutorConfig(objectName);
        durableExecutorConfig.setNamespace(getNamespaceName());

        super.setUpInstance();

        executor = instance.getDurableExecutorService(objectName);
    }

    @Override
    protected void mutateConfig(Config config) {
        config.addDurableExecutorConfig(durableExecutorConfig);
    }
}