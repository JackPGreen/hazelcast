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

package com.hazelcast.test.archunit;

import org.junit.BeforeClass;

import java.text.MessageFormat;

import static org.assertj.core.api.Assumptions.assumeThat;

public abstract class ArchUnitTestSupport {

    private static final int HIGHEST_JDK = 24;

    // ArchUnit releases lag behind the JDK releases.
    // Skip the test if JDK version is higher than the specified assumption
    @BeforeClass
    public static void beforeClass() {
        assumeThat(getMajorJavaVersion())
                .as(MessageFormat.format(
                        "ArchUnit {1} supports Java {0} or below - https://github.com/TNG/ArchUnit/releases/tag/v{1}",
                        HIGHEST_JDK, "1.4.0"))
                .isLessThanOrEqualTo(HIGHEST_JDK);
    }

    private static int getMajorJavaVersion() {
        return Runtime.version().feature();
    }
}
