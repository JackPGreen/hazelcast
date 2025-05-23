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

package com.hazelcast.spi.impl;

import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.concurrent.CancellationException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class AbstractInvocationFuture_CancelTest extends AbstractInvocationFuture_AbstractTest {

    @Test
    public void whenCancelCalled_thenFutureCancelled() {
        // When
        final boolean result = future.cancel(true);

        // Then
        assertTrue(result);
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test
    public void whenCancelled_thenCantCancelAgain() {
        // When
        future.cancel(true);

        // Then
        assertFalse(future.cancel(true));
    }

    @Test
    public void whenCancelled_thenGetThrowsCancelled() {
        // Given
        future.cancel(true);

        // Then
        assertThrows(CancellationException.class, () -> future.get());
    }

    @Test
    public void whenCancelled_thenJoinThrowsCancelled() {
        // Given
        future.cancel(true);

        // Then
        assertThrows(CancellationException.class, () -> future.join());
    }

    @Test
    public void whenCancelled_thenCompleteNoEffect() {
        // Given
        future.cancel(true);

        // When
        future.complete(value);

        // Then
        assertThrows(CancellationException.class, () -> future.join());
    }
}
