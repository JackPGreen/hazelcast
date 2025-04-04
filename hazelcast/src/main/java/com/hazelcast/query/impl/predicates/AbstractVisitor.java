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

package com.hazelcast.query.impl.predicates;

import com.hazelcast.query.Predicate;
import com.hazelcast.query.impl.IndexRegistry;

/**
 * Base class for all visitors. It returns the original predicate without touching.
 * Concrete Visitor can just override the method for predicate the visitor is interested in.
 */
public abstract class AbstractVisitor implements Visitor {

    @Override
    public Predicate visit(EqualPredicate predicate, IndexRegistry indexes) {
        return predicate;
    }

    @Override
    public Predicate visit(NotEqualPredicate predicate, IndexRegistry indexes) {
        return predicate;
    }

    @Override
    public Predicate visit(AndPredicate predicate, IndexRegistry indexes) {
        return predicate;
    }

    @Override
    public Predicate visit(OrPredicate predicate, IndexRegistry indexes) {
        return predicate;
    }

    @Override
    public Predicate visit(NotPredicate predicate, IndexRegistry indexes) {
        return predicate;
    }

    @Override
    public Predicate visit(InPredicate predicate, IndexRegistry indexes) {
        return predicate;
    }

    @Override
    public Predicate visit(BetweenPredicate predicate, IndexRegistry indexes) {
        return predicate;
    }

}
