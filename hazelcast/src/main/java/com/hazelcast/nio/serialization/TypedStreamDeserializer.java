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

package com.hazelcast.nio.serialization;

import com.hazelcast.nio.ObjectDataInput;

import java.io.IOException;

/**
 * This interface allows deserialization of a binary data with a provided class type.
 */
@FunctionalInterface
@SuppressWarnings("JavadocType")
public interface TypedStreamDeserializer<T>  {
    /**
     * Reads object from objectDataInputStream
     *
     * @param in ObjectDataInput stream that object will read from
     * @param aClass The class to use for de-serialization
     * @return read object
     * @throws IOException in case of failure to read
     */
    T read(ObjectDataInput in, Class aClass) throws IOException;
}
