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

package com.hazelcast.internal.namespace.querycache;

import com.hazelcast.config.Config;
import com.hazelcast.config.QueryCacheConfig;
import com.hazelcast.internal.namespace.imap.IMapUCDTest;
import com.hazelcast.map.QueryCache;

public abstract class QueryCacheUCDTest extends IMapUCDTest {
    protected QueryCacheConfig queryCacheConfig;
    protected QueryCache<Object, Object> cache;

    @Override
    public void setUpInstance() throws ReflectiveOperationException {
        String cacheName = randomName();

        queryCacheConfig = new QueryCacheConfig(cacheName);
        queryCacheConfig.setNamespace(getNamespaceName());

        super.setUpInstance();

        cache = member.getMap(objectName).getQueryCache(cacheName);
    }

    @Override
    protected void mutateConfig(Config config) {
        mapConfig.addQueryCacheConfig(queryCacheConfig);

        super.mutateConfig(config);
    }
}
