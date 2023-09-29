package com.hazelcast.internal.util.primitivefunctions;

/** {@link char} version of {@link ToIntFunction} */
@FunctionalInterface
public interface ToCharFunction<T> {
    char applyAsChar(T value);
}