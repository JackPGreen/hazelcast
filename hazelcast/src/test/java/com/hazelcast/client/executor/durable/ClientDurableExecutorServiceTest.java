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

package com.hazelcast.client.executor.durable;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.client.test.TestHazelcastFactory;
import com.hazelcast.client.test.executor.tasks.FailingCallable;
import com.hazelcast.config.Config;
import com.hazelcast.config.DurableExecutorConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.durableexecutor.DurableExecutorService;
import com.hazelcast.durableexecutor.DurableExecutorServiceFuture;
import com.hazelcast.executor.ExecutorServiceTestSupport.BasicTestCallable;
import com.hazelcast.executor.ExecutorServiceTestSupport.SleepingTask;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.hazelcast.internal.util.RootCauseMatcher.rootCause;
import static com.hazelcast.test.HazelcastTestSupport.assertOpenEventually;
import static com.hazelcast.test.HazelcastTestSupport.assertTrueEventually;
import static com.hazelcast.test.HazelcastTestSupport.randomString;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class ClientDurableExecutorServiceTest {

    private static final String SINGLE_TASK = "singleTask";

    private final TestHazelcastFactory hazelcastFactory = new TestHazelcastFactory();
    private HazelcastInstance client;

    @Before
    public void setup()
            throws IOException {
        Config config = new XmlConfigBuilder(getClass().getClassLoader().getResourceAsStream("hazelcast-test-executor.xml"))
                .build();
        ClientConfig clientConfig = new XmlClientConfigBuilder("classpath:hazelcast-client-test-executor.xml").build();
        DurableExecutorConfig durableExecutorConfig = config.getDurableExecutorConfig(SINGLE_TASK + "*");
        durableExecutorConfig.setCapacity(1);

        hazelcastFactory.newHazelcastInstance(config);
        hazelcastFactory.newHazelcastInstance(config);
        hazelcastFactory.newHazelcastInstance(config);
        hazelcastFactory.newHazelcastInstance(config);
        client = hazelcastFactory.newHazelcastClient(clientConfig);
    }

    @After
    public void tearDown() {
        hazelcastFactory.terminateAll();
    }

    @Test
    public void testInvokeAll() {
        DurableExecutorService service = client.getDurableExecutorService(randomString());
        List<BasicTestCallable> callables = Collections.emptyList();

        assertThatThrownBy(() -> service.invokeAll(callables)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testInvokeAll_WithTimeout() {
        DurableExecutorService service = client.getDurableExecutorService(randomString());
        List<BasicTestCallable> callables = Collections.emptyList();

        assertThatThrownBy(() -> service.invokeAll(callables, 1, TimeUnit.SECONDS)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testInvokeAny() {
        DurableExecutorService service = client.getDurableExecutorService(randomString());
        List<BasicTestCallable> callables = Collections.emptyList();

        assertThatThrownBy(() -> service.invokeAny(callables)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testInvokeAny_WithTimeout() {
        DurableExecutorService service = client.getDurableExecutorService(randomString());
        List<BasicTestCallable> callables = Collections.emptyList();

        assertThatThrownBy(() -> service.invokeAny(callables, 1, TimeUnit.SECONDS))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testAwaitTermination() throws Exception {
        DurableExecutorService service = client.getDurableExecutorService(randomString());
        assertFalse(service.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void test_whenRingBufferIsFull_thenThrowRejectedExecutionException() {
        String key = randomString();
        DurableExecutorService service = client.getDurableExecutorService(SINGLE_TASK + randomString());
        service.submitToKeyOwner(new SleepingTask(100), key);
        DurableExecutorServiceFuture<String> future = service.submitToKeyOwner(new BasicTestCallable(), key);

        assertThatThrownBy(future::get).has(rootCause(RejectedExecutionException.class));
    }

    @Test
    public void test_whenRingBufferIsFull_thenClientDurableExecutorServiceCompletedFutureIsReturned() throws Exception {
        final AtomicBoolean onResponse = new AtomicBoolean();
        final CountDownLatch onFailureLatch = new CountDownLatch(1);

        String key = randomString();
        DurableExecutorService service = client.getDurableExecutorService(SINGLE_TASK + randomString());
        service.submitToKeyOwner(new SleepingTask(100), key);
        DurableExecutorServiceFuture<String> future = service.submitToKeyOwner(new BasicTestCallable(), key);
        future.whenCompleteAsync((response, t) -> {
            if (t == null) {
                onResponse.set(true);
            } else {
                onFailureLatch.countDown();
            }
        });

        try {
            future.get(1, TimeUnit.HOURS);
            fail("We expected that future.get() throws an ExecutionException!");
        } catch (ExecutionException ignored) {
        }

        // assert TaskId
        try {
            future.getTaskId();
            fail("We expected that future.getTaskId() throws an IllegalStateException!");
        } catch (IllegalStateException ignored) {
        }

        // assert states of ClientDurableExecutorServiceCompletedFuture
        assertFalse(future.cancel(false));
        assertFalse(future.cancel(true));
        assertFalse(future.isCancelled());
        assertTrue(future.isDone());

        // assert that onFailure() has been called and not onResponse()
        onFailureLatch.await();
        assertFalse(onResponse.get());
    }

    public void testFullRingBuffer_WithExecutionCallback() {
        String key = randomString();
        DurableExecutorService service = client.getDurableExecutorService(SINGLE_TASK + randomString());
        service.submitToKeyOwner(new SleepingTask(100), key);
        DurableExecutorServiceFuture<String> future = service.submitToKeyOwner(new BasicTestCallable(), key);
        final CountDownLatch latch = new CountDownLatch(1);
        future.exceptionally(t -> {
            if (t.getCause() instanceof RejectedExecutionException) {
                latch.countDown();
            }
            return null;
        });
        assertOpenEventually(latch);
        assertTrue(future.isDone());
        assertFalse(future.cancel(true));
        assertFalse(future.isCancelled());
    }

    @Test
    public void testIsTerminated() {
        DurableExecutorService service = client.getDurableExecutorService(randomString());

        assertFalse(service.isTerminated());
    }

    @Test
    public void testIsShutdown() {
        DurableExecutorService service = client.getDurableExecutorService(randomString());

        assertFalse(service.isShutdown());
    }

    @Test
    public void testShutdownNow() {
        final DurableExecutorService service = client.getDurableExecutorService(randomString());

        service.shutdownNow();

        assertTrueEventually(() -> assertTrue(service.isShutdown()));
    }

    @Test
    public void testShutdownMultipleTimes() {
        final DurableExecutorService service = client.getDurableExecutorService(randomString());
        service.shutdownNow();
        service.shutdown();

        assertTrueEventually(() -> assertTrue(service.isShutdown()));
    }

    @Test
    public void testSubmitFailingCallableException() {
        DurableExecutorService service = client.getDurableExecutorService(randomString());
        Future<String> failingFuture = service.submit(new FailingCallable());

        assertThatThrownBy(failingFuture::get).isInstanceOf(ExecutionException.class);
    }

    @Test
    public void testSubmitFailingCallableException_withExecutionCallback() throws Exception {
        DurableExecutorService service = client.getDurableExecutorService(randomString());
        final CountDownLatch latch = new CountDownLatch(1);
        service.submit(new FailingCallable()).exceptionally(t -> {
            latch.countDown();
            return null;
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testSubmitFailingCallableReasonExceptionCause() {
        DurableExecutorService service = client.getDurableExecutorService(randomString());
        Future<String> future = service.submit(new FailingCallable());

        assertThatThrownBy(future::get).has(rootCause(IllegalStateException.class));
    }

    @Test
    public void testCallableSerializedOnce() throws Exception {
        String name = randomString();
        DurableExecutorService service = client.getDurableExecutorService(name);
        SerializedCounterCallable counterCallable = new SerializedCounterCallable();

        Future<Integer> future = service.submitToKeyOwner(counterCallable, name);
        assertEquals(Integer.valueOf(2), future.get());
    }

    private static class SerializedCounterCallable implements Callable<Integer>, DataSerializable {

        int counter;

        SerializedCounterCallable() {
        }

        @Override
        public Integer call() {
            return counter;
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeInt(++counter);
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            counter = in.readInt() + 1;
        }
    }
}
