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

package com.hazelcast.internal.ascii.memcache;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static com.hazelcast.internal.ascii.memcache.MemcacheCommandProcessor.DEFAULT_MAP_NAME;
import static com.hazelcast.internal.ascii.memcache.MemcacheCommandProcessor.MAP_NAME_PREFIX;

public final class MemcacheUtils {

    private MemcacheUtils() {
    }

    /**
     * Parse Memcache key into (MapName, Key) pair.
     */
    public static MapNameAndKeyPair parseMemcacheKey(String key) {
        key = URLDecoder.decode(key, StandardCharsets.UTF_8);
        String mapName = DEFAULT_MAP_NAME;
        int index = key.indexOf(':');
        if (index != -1) {
            mapName = MAP_NAME_PREFIX + key.substring(0, index);
            key = key.substring(index + 1);
        }
        return new MapNameAndKeyPair(mapName, key);
    }
}
