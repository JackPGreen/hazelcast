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

package com.hazelcast.internal.namespace.imap;

import com.hazelcast.config.Config;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class IMapMapLoaderUCDTest extends IMapUCDTest {
    @Test
    public void test() throws Exception {
        assertFalse(map.isEmpty());
    }

    @Override
    protected String[] getUserDefinedClassNames() {
        return new String[] {"usercodedeployment.KeyBecomesValueMapLoader"};
    }

    @Override
    protected void mutateConfig(Config config) {
        mapConfig.getMapStoreConfig().setEnabled(true).setClassName(getUserDefinedClassNames()[0]);

        super.mutateConfig(config);
    }
}