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

package com.hazelcast.query.impl;

/**
 * Provides the query context support for global indexes with disabled stats.
 */
public class GlobalQueryContextProvider implements QueryContextProvider {

    private static final ThreadLocal<QueryContext> QUERY_CONTEXT = ThreadLocal.withInitial(() -> new QueryContext());

    @Override
    public QueryContext obtainContextFor(IndexRegistry indexes, int ownedPartitionCount) {
        QueryContext queryContext = QUERY_CONTEXT.get();
        queryContext.attachTo(indexes, ownedPartitionCount);
        return queryContext;
    }

}
