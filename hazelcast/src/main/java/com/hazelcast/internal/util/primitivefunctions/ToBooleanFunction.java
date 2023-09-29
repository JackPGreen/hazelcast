package com.hazelcast.internal.util.primitivefunctions;

/** {@link boolean} version of {@link ToIntFunction} */
@FunctionalInterface
public interface ToBooleanFunction<T> {
    boolean applyAsBoolean(T value);
}