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

package com.hazelcast.map.impl.mapstore.writebehind;

import java.io.Serial;
import java.util.EventObject;

/**
 * For internal usage only.
 *
 * @param <E>
 */
public final class StoreEvent<E> extends EventObject {

    @Serial
    private static final long serialVersionUID = -7071512331813330032L;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    private StoreEvent(E source) {
        super(source);
    }

    public static <E> StoreEvent<E> createStoreEvent(E source) {
        return new StoreEvent<>(source);
    }

    @Override
    public E getSource() {
        return (E) super.getSource();
    }
}
