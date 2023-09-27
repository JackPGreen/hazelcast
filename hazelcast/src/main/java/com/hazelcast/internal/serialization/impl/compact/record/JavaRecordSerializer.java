/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.internal.serialization.impl.compact.record;

import com.hazelcast.internal.serialization.impl.compact.CompactStreamSerializer;
import com.hazelcast.internal.serialization.impl.compact.CompactUtil;
import com.hazelcast.internal.serialization.impl.compact.DefaultCompactReader;
import com.hazelcast.internal.serialization.impl.compact.Schema;
import com.hazelcast.internal.serialization.impl.compact.zeroconfig.ValueReaderWriter;
import com.hazelcast.internal.serialization.impl.compact.zeroconfig.ValueReaderWriters;
import com.hazelcast.internal.util.ExceptionUtil;
import com.hazelcast.nio.serialization.HazelcastSerializationException;
import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

import javax.annotation.Nonnull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

// TODO is there any tests for *any* of this...?!
@SuppressWarnings("checkstyle:executablestatementcount")
public class JavaRecordSerializer implements CompactSerializer<Object> {
    private static final Lookup LOOKUP = MethodHandles.lookup();

    private static final MethodHandle isRecordMethod;
    private static final MethodHandle getRecordComponentsMethod;
    private static final MethodHandle getTypeMethod;
    private static final MethodHandle getNameMethod;
    private static final MethodHandle getGenericTypeMethod;
    private final CompactStreamSerializer compactStreamSerializer;
    private final Map<Class<?>, JavaRecordReader> readersCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, ComponentReaderWriter[]> readerWritersCache = new ConcurrentHashMap<>();
    
    static {
        MethodHandle isRecordMethodLocal;
        MethodHandle getRecordComponentsMethodLocal;
        MethodHandle getTypeMethodLocal;
        MethodHandle getNameMethodLocal;
        MethodHandle getGenericTypeMethodLocal;

        try {
            isRecordMethodLocal = LOOKUP.unreflect(Class.class.getMethod("isRecord"));
            getRecordComponentsMethodLocal = LOOKUP.unreflect(Class.class.getMethod("getRecordComponents"));

            Class<?> recordComponentClass = Class.forName("java.lang.reflect.RecordComponent");
            getTypeMethodLocal = LOOKUP.unreflect(recordComponentClass.getMethod("getType"));
            getNameMethodLocal = LOOKUP.unreflect(recordComponentClass.getMethod("getName"));
            getGenericTypeMethodLocal = LOOKUP.unreflect(recordComponentClass.getMethod("getGenericType"));
        } catch (Throwable t) {
            isRecordMethodLocal = null;
            getRecordComponentsMethodLocal = null;
            getTypeMethodLocal = null;
            getNameMethodLocal = null;
            getGenericTypeMethodLocal = null;
        }

        isRecordMethod = isRecordMethodLocal;
        getRecordComponentsMethod = getRecordComponentsMethodLocal;
        getTypeMethod = getTypeMethodLocal;
        getNameMethod = getNameMethodLocal;
        getGenericTypeMethod = getGenericTypeMethodLocal;
    }

    public JavaRecordSerializer(CompactStreamSerializer compactStreamSerializer) {
        this.compactStreamSerializer = compactStreamSerializer;
    }

    public boolean isRecord(Class<?> clazz) {
        if (isRecordMethod == null) {
            return false;
        }

        try {
            return (boolean) isRecordMethod.invoke(clazz);
        } catch (Throwable t) {
            return false;
        }
    }

    @Nonnull
    @Override
    public Object read(@Nonnull CompactReader reader) {
        DefaultCompactReader compactReader = (DefaultCompactReader) reader;
        Class<?> associatedClass = requireNonNull(compactReader.getAssociatedClass(),
                "AssociatedClass is required for JavaRecordSerializer");

        JavaRecordReader recordReader = readersCache.get(associatedClass);
        if (recordReader == null) {
            populateReadersWriters(associatedClass);
            recordReader = readersCache.get(associatedClass);
        }

        return recordReader.readRecord(compactReader, compactReader.getSchema());
    }

    @Override
    public void write(@Nonnull CompactWriter writer, @Nonnull Object object) {
        Class<?> clazz = object.getClass();
        ComponentReaderWriter[] readerWriters = readerWritersCache.get(clazz);
        if (readerWriters == null) {
            populateReadersWriters(clazz);
            readerWriters = readerWritersCache.get(clazz);
        }

        try {
            for (ComponentReaderWriter readerWriter : readerWriters) {
                readerWriter.writeComponent(writer, object);
            }
        } catch (Exception e) {
            throw new HazelcastSerializationException("Failed to write the Java record", e);
        }
    }

    @Nonnull
    @Override
    public String getTypeName() {
        throw new IllegalStateException("getTypeName should not be called for the record serializer");
    }

    @Nonnull
    @Override
    public Class<Object> getCompactClass() {
        throw new IllegalStateException("getCompactClass should not be called for the record serializer");
    }

    private void populateReadersWriters(Class<?> clazz) {
        // The top level class might not be Compact serializable
        CompactUtil.verifyClassIsCompactSerializable(clazz);

        try {
            Object[] recordComponents = (Object[]) getRecordComponentsMethod.invoke(clazz);
            Class<?>[] componentTypes = new Class<?>[recordComponents.length];

            ComponentReaderWriter[] componentReaderWriters = new ComponentReaderWriter[recordComponents.length];

            for (int i = 0; i < recordComponents.length; i++) {
                Object recordComponent = recordComponents[i];
                Class<?> type = (Class<?>) getTypeMethod.invoke(recordComponent);
                Type genericType = (Type) getGenericTypeMethod.invoke(recordComponent);
                String name = (String) getNameMethod.invoke(recordComponent);

                componentTypes[i] = type;
                Method componentGetter = clazz.getDeclaredMethod(name);
                componentGetter.setAccessible(true);
                componentReaderWriters[i] = new ComponentReaderWriterAdapter(
                        ValueReaderWriters.readerWriterFor(compactStreamSerializer, clazz, type, genericType, name),
                        LOOKUP.unreflect(componentGetter)
                );
            }

            Constructor<?> constructor = clazz.getDeclaredConstructor(componentTypes);
            constructor.setAccessible(true);

            JavaRecordReader recordReader = new JavaRecordReader(constructor, componentReaderWriters);
            readersCache.put(clazz, recordReader);
            readerWritersCache.put(clazz, componentReaderWriters);
        } catch (Throwable t) {
            throw new HazelcastSerializationException("Failed to construct the readers/writers for the Java record", t);
        }
    }

    private static final class ComponentReaderWriterAdapter implements ComponentReaderWriter {

        private final ValueReaderWriter readerWriter;
        private final MethodHandle componentGetter;

        private ComponentReaderWriterAdapter(ValueReaderWriter readerWriter, MethodHandle componentGetter) {
            this.readerWriter = readerWriter;
            this.componentGetter = componentGetter;
        }

        @Override
        public Object readComponent(CompactReader compactReader, Schema schema) {
            return readerWriter.read(compactReader, schema);
        }

        @Override
        public void writeComponent(CompactWriter compactWriter, Object recordObject) throws Exception {
            try {
                readerWriter.write(compactWriter, componentGetter.invoke(recordObject));
            } catch (Exception e) {
                throw e;
            } catch (Throwable t) {
                ExceptionUtil.sneakyThrow(t);
            }
        }
    }
}
