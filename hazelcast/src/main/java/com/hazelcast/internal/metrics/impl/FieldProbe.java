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

package com.hazelcast.internal.metrics.impl;

import static com.hazelcast.internal.metrics.impl.ProbeType.getType;
import static java.lang.String.format;

import com.hazelcast.internal.metrics.DoubleProbeFunction;
import com.hazelcast.internal.metrics.LongProbeFunction;
import com.hazelcast.internal.metrics.MetricDescriptor;
import com.hazelcast.internal.metrics.Probe;
import com.hazelcast.internal.metrics.ProbeFunction;
import com.hazelcast.internal.util.counters.Counter;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * A FieldProbe is a {@link ProbeFunction} that reads out a field that is annotated with {@link Probe}.
 */
abstract class FieldProbe implements ProbeFunction {
    // TODO Move this + other shared MethodProbe code into ProbeFunction?
    private static final Lookup LOOKUP = MethodHandles.lookup();

    final VarHandle varHandle;
    final boolean isFieldStatic;
    final CachedProbe probe;
    final ProbeType type;
    final SourceMetadata sourceMetadata;
    final String probeName;

    FieldProbe(final Field field, final Probe probe, final ProbeType type, final SourceMetadata sourceMetadata) {
        try {
            varHandle = MethodHandles.privateLookupIn(field.getDeclaringClass(), LOOKUP).unreflectVarHandle(field);

            isFieldStatic = Modifier.isStatic(field.getModifiers());

            this.probe = new CachedProbe(probe);
            this.type = type;
            this.sourceMetadata = sourceMetadata;
            probeName = probe.name();
            assert probeName != null;
            assert probeName.length() > 0;
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    void register(final MetricsRegistryImpl metricsRegistry, final Object source, final String namePrefix) {
        final MetricDescriptor descriptor = metricsRegistry.newMetricDescriptor().withPrefix(namePrefix)
                .withMetric(getProbeName());
        metricsRegistry.registerInternal(source, descriptor, probe.level(), this);
    }

    void register(final MetricsRegistryImpl metricsRegistry, final MetricDescriptor descriptor, final Object source) {
        metricsRegistry.registerStaticProbe(source, descriptor, getProbeName(), probe.level(), probe.unit(), this);
    }

    String getProbeName() {
        return probeName;
    }

    static <S> FieldProbe createFieldProbe(final Field field, final Probe probe, final SourceMetadata sourceMetadata) {
        final ProbeType type = getType(field.getType());
        if (type == null) {
            throw new IllegalArgumentException(format("@Probe field '%s' is of an unhandled type", field));
        }

        if (type.getMapsTo() == double.class) {
            return new DoubleFieldProbe<S>(field, probe, type, sourceMetadata);
        } else if (type.getMapsTo() == long.class) {
            return new LongFieldProbe<S>(field, probe, type, sourceMetadata);
        } else {
            throw new IllegalArgumentException(type.toString());
        }
    }

    static class LongFieldProbe<S> extends FieldProbe implements LongProbeFunction<S> {

        LongFieldProbe(final Field field, final Probe probe, final ProbeType type, final SourceMetadata sourceMetadata) {
            super(field, probe, type, sourceMetadata);
        }

        @Override
        public long get(final S source) throws Exception {
            switch (type) {
                case TYPE_LONG_PRIMITIVE:
                    return isFieldStatic ? (long) varHandle.get() : (long) varHandle.get(source);
                case TYPE_LONG_NUMBER:
                    final Number longNumber = (Number) getFromField(source);
                    return longNumber == null ? 0 : longNumber.longValue();
                case TYPE_MAP:
                    final Map<?, ?> map = (Map<?, ?>) getFromField(source);
                    return map == null ? 0 : map.size();
                case TYPE_COLLECTION:
                    final Collection<?> collection = (Collection<?>) getFromField(source);
                    return collection == null ? 0 : collection.size();
                case TYPE_COUNTER:
                    final Counter counter = (Counter) getFromField(source);
                    return counter == null ? 0 : counter.get();
                case TYPE_SEMAPHORE:
                    final Semaphore semaphore = (Semaphore) getFromField(source);
                    return semaphore == null ? 0 : semaphore.availablePermits();
                default:
                    throw new IllegalStateException("Unhandled type:" + type);
            }
        }
    }

    static class DoubleFieldProbe<S> extends FieldProbe implements DoubleProbeFunction<S> {

        DoubleFieldProbe(final Field field, final Probe probe, final ProbeType type, final SourceMetadata sourceMetadata) {
            super(field, probe, type, sourceMetadata);
        }

        @Override
        public double get(final S source) throws Exception {
            switch (type) {
                case TYPE_DOUBLE_PRIMITIVE:
                    return isFieldStatic ? (double) varHandle.get() : (double) varHandle.get(source);
                case TYPE_DOUBLE_NUMBER:
                    final Number doubleNumber = (Number) getFromField(source);
                    return doubleNumber == null ? 0 : doubleNumber.doubleValue();
                default:
                    throw new IllegalStateException("Unhandled type:" + type);
            }
        }
    }

    protected <T> T getFromField(final Object source) {
        return isFieldStatic ? (T) varHandle.get() : (T) varHandle.get(source);
    }
}
