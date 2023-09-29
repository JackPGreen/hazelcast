package com.hazelcast.internal.util.primitivefunctions;

/** {@link float} version of {@link ToIntFunction} */
@FunctionalInterface
public interface ToFloatFunction<T> {
    float applyAsFloat(T value);
}