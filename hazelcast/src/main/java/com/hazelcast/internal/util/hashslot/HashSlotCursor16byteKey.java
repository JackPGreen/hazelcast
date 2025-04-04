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

package com.hazelcast.internal.util.hashslot;

/**
 * Cursor over assigned slots in a {@link HashSlotArray16byteKey}. Initially the cursor's location is
 * before the first map entry and the cursor is invalid.
 */
public interface HashSlotCursor16byteKey extends HashSlotCursor {

    /**
     * @return key part 1 of current slot.
     * @throws IllegalStateException if the cursor is invalid.
     */
    long key1();

    /**
     * @return key part 2 of current slot.
     * @throws IllegalStateException if the cursor is invalid.
     */
    long key2();
}
