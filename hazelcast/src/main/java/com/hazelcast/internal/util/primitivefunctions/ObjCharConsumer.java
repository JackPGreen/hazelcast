package com.hazelcast.internal.util.primitivefunctions;

/** {@link char} version of {@link ObjIntConsumer} */
@FunctionalInterface
public interface ObjCharConsumer<T> {
    void accept(T t, char value);
}