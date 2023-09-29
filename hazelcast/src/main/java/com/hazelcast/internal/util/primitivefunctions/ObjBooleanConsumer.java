package com.hazelcast.internal.util.primitivefunctions;

/** {@link boolean} version of {@link ObjIntConsumer} */
@FunctionalInterface
public interface ObjBooleanConsumer<T> {
    void accept(T t, boolean value);
}