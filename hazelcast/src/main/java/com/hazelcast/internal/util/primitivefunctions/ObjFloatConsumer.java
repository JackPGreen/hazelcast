package com.hazelcast.internal.util.primitivefunctions;

/** {@link float} version of {@link ObjIntConsumer} */
@FunctionalInterface
public interface ObjFloatConsumer<T> {
    void accept(T t, float value);
}