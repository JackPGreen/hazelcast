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

package com.hazelcast.spi.impl.operationexecutor.impl;

import com.hazelcast.internal.tpcengine.Eventloop;
import com.hazelcast.internal.tpcengine.Scheduler;

import static java.util.concurrent.TimeUnit.MICROSECONDS;

/**
 * The Scheduler for TPC. So each reactor contains an partition-operation thread
 * and each of these threads runs an eventloop which contains a scheduler. This
 * scheduler is given a tick on every run of the eventloop to do some work. In
 * case of the TPC, we process of a batch of operations from the operation-queue
 * and then hand control back to the eventloop.
 */
public class TpcOperationScheduler implements Scheduler {

    private static final int TIME_SLICE_US_DEFAULT = 500;
    private static final String TIME_SLICE_US_NAME = "hazelcast.internal.tpc.timeSliceUs";

    private TpcPartitionOperationThread operationThread;
    private OperationQueue queue;
    private final long timeSliceNs;

    public TpcOperationScheduler() {
        long timeSliceUs = Integer.getInteger(TIME_SLICE_US_NAME, TIME_SLICE_US_DEFAULT);
        this.timeSliceNs = MICROSECONDS.toNanos(timeSliceUs);
    }

    @Override
    public void init(Eventloop eventloop) {
        // This method is guaranteed to be called from the Reactor thread (which
        // is the TpcPartitionOperationThread).
        this.operationThread = (TpcPartitionOperationThread) Thread.currentThread();
        this.queue = operationThread.queue;
    }

    @Override
    public boolean tick() {
        final TpcPartitionOperationThread operationThread0 = operationThread;
        final OperationQueue queue0 = queue;
        final long timeSliceNs0 = timeSliceNs;

        long startNs = System.nanoTime();
        do {
            if (operationThread0.isShutdown()) {
                return false;
            }

            Object task = queue0.poll();
            if (task == null) {
                return false;
            }

            operationThread0.process(task);
        } while (System.nanoTime() - startNs < timeSliceNs0);

        return !queue0.isEmpty();
    }

    @Override
    public void schedule(Object task) {
        // the OperationExecutor moves tasks directly into the OperationQueue.
        throw new UnsupportedOperationException();
    }
}
