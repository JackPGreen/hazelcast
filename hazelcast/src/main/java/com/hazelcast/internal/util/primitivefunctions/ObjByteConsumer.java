package com.hazelcast.internal.util.primitivefunctions;

/** {@link byte} version of {@link ObjIntConsumer} */
@FunctionalInterface
public interface ObjByteConsumer<T> {
    void accept(T t, byte value);
}