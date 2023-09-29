package com.hazelcast.internal.util.primitivefunctions;

/** {@link short} version of {@link ToIntFunction} */
@FunctionalInterface
public interface ToShortFunction<T> {
    short applyAsShort(T value);
}