/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.test.annotation;

import com.hazelcast.test.TestEnvironment;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Mark a test as a compatibility test between open-source and enterprise editions.
 * Similar to {@link CompatibilityTest}, but we need a separate annotation because some features
 * tested in compatibility tests are not supported in open-source edition.
 * <p>
 * <p>When executing a compatibility test, there are limitations, the most important being:
 * <ul>
 * <li>Only use public API. Code that relies on Hazelcast internals, like obtaining a reference to the {@code Node}
 * in a {@code HazelcastInstance}, will only succeed when executed on a {@code HazelcastInstance} of the current
 * version.</li>
 * <li>Test classes used as domain classes, listeners, entry processors etc, when implementing {@code Serializable}
 * must specify a {@code serialVersionUID}, otherwise failures in serialization/deserialization will occur.</li>
 * </ul>
 * </p>
 *
 * @see TestEnvironment#isRunningCompatibilityTest()
 */
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Tag("com.hazelcast.test.annotation.OSToEECompatibilityTest")
public @interface OSToEECompatibilityTest {
}
