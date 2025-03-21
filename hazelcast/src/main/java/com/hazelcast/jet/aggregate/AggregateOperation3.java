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

package com.hazelcast.jet.aggregate;

import com.hazelcast.function.BiConsumerEx;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.core.Processor;

import javax.annotation.Nonnull;

/**
 * Specialization of {@code AggregateOperation} (refer to its {@linkplain
 * AggregateOperation extensive documentation}) to the "arity-3" case with
 * three data streams being aggregated over. {@link AggregateOperations}
 * contains factories for the built-in implementations, and you can create
 * your own using the {@linkplain AggregateOperation#withCreate aggregate
 * operation builder}.
 * <p>
 * This example constructs an operation that sums up {@code long} values
 * from three streams:
 * <pre>{@code
 * AggregateOperation3<Long, Long, Long, LongAccumulator, Long> aggrOp = AggregateOperation
 *     .withCreate(LongAccumulator::new)
 *     .<Long>andAccumulate0(LongAccumulator::add)
 *     .<Long>andAccumulate1(LongAccumulator::add)
 *     .<Long>andAccumulate2(LongAccumulator::add)
 *     .andFinish(LongAccumulator::get);
 * }</pre>
 * <p>
 * All the functions must be stateless and {@linkplain
 * Processor#isCooperative() cooperative}.
 *
 * @param <T0> the type of item in stream-0
 * @param <T1> the type of item in stream-1
 * @param <T2> the type of item in stream-2
 * @param <A> the type of the accumulator
 * @param <R> the type of the aggregation result
 *
 * @since Jet 3.0
 */
public interface AggregateOperation3<T0, T1, T2, A, R> extends AggregateOperation<A, R> {

    /**
     * A primitive that updates the accumulator state to account for a new
     * item coming from stream-0.
     * <p>
     * The consumer must be stateless and {@linkplain Processor#isCooperative()
     * cooperative}.
     */
    @Nonnull
    BiConsumerEx<? super A, ? super T0> accumulateFn0();

    /**
     * A primitive that updates the accumulator state to account for a new
     * item coming from stream-1.
     * <p>
     * The consumer must be stateless and {@linkplain Processor#isCooperative()
     * cooperative}.
     */
    @Nonnull
    BiConsumerEx<? super A, ? super T1> accumulateFn1();

    /**
     * A primitive that updates the accumulator state to account for a new
     * item coming from stream-2.
     * <p>
     * The consumer must be stateless and {@linkplain Processor#isCooperative()
     * cooperative}.
     */
    @Nonnull
    BiConsumerEx<? super A, ? super T2> accumulateFn2();

    /**
     * Returns a copy of this aggregate operation, but with the {@code
     * accumulate} primitive at index 0 replaced with the one supplied here.
     * <p>
     * The consumer must be stateless and {@linkplain Processor#isCooperative()
     * cooperative}.
     */
    @Nonnull
    <T0_NEW> AggregateOperation3<T0_NEW, T1, T2, A, R> withAccumulateFn0(
            @Nonnull BiConsumerEx<? super A, ? super T0_NEW> newAccFn0
    );

    /**
     * Returns a copy of this aggregate operation, but with the {@code
     * accumulate} primitive at index 1 replaced with the one supplied here.
     * <p>
     * The consumer must be stateless and {@linkplain Processor#isCooperative()
     * cooperative}.
     */
    @Nonnull
    <T1_NEW> AggregateOperation3<T0, T1_NEW, T2, A, R> withAccumulateFn1(
            @Nonnull BiConsumerEx<? super A, ? super T1_NEW> newAccFn1
    );

    /**
     * Returns a copy of this aggregate operation, but with the {@code
     * accumulate} primitive at index 2 replaced with the one supplied here.
     * <p>
     * The consumer must be stateless and {@linkplain Processor#isCooperative()
     * cooperative}.
     */
    @Nonnull
    <T2_NEW> AggregateOperation3<T0, T1, T2_NEW, A, R> withAccumulateFn2(
            @Nonnull BiConsumerEx<? super A, ? super T2_NEW> newAccFn2
    );

    // Narrows the return type
    @Nonnull @Override
    AggregateOperation3<T0, T1, T2, A, A> withIdentityFinish();

    // Narrows the return type
    @Nonnull @Override
    <R_NEW> AggregateOperation3<T0, T1, T2, A, R_NEW> andThen(FunctionEx<? super R, ? extends R_NEW> thenFn);
}
