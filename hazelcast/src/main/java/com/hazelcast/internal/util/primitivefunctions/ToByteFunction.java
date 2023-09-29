package com.hazelcast.internal.util.primitivefunctions;

/** {@link byte} version of {@link ToIntFunction} */
@FunctionalInterface
public interface ToByteFunction<T> {
    byte applyAsByte(T value);
}