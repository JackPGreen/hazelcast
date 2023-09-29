package com.hazelcast.internal.util.primitivefunctions;

/** {@link short} version of {@link ObjIntConsumer} */
@FunctionalInterface
public interface ObjShortConsumer<T> {
    void accept(T t, short value);
}