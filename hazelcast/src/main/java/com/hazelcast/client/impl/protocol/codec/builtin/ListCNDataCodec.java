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

package com.hazelcast.client.impl.protocol.codec.builtin;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.internal.serialization.Data;

import java.util.Collection;
import java.util.List;

/**
 * Codec for the list of data which allows optional items
 */
public final class ListCNDataCodec {

    private ListCNDataCodec() {
    }

    public static void encode(ClientMessage clientMessage, Collection<Data> collection) {
        ListMultiFrameCodec.encode(clientMessage, collection, DataCodec::encodeNullable);
    }

    public static List<Data> decode(ClientMessage.ForwardFrameIterator iterator) {
        return ListMultiFrameCodec.decode(iterator, DataCodec::decodeNullable);
    }
}
