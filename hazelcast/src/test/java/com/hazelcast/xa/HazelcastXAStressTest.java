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

package com.hazelcast.xa;

import com.atomikos.datasource.xa.XID;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.transaction.TransactionalMap;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import com.hazelcast.test.annotation.NightlyTest;
import com.hazelcast.transaction.HazelcastXAResource;
import com.hazelcast.transaction.TransactionContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(HazelcastSerialClassRunner.class)
@Category(NightlyTest.class)
public class HazelcastXAStressTest extends HazelcastTestSupport {

    private HazelcastInstance instance;

    private static Xid createXid() {
        return new XID(randomString(), "test");
    }

    @Before
    public void setUp() throws Exception {
        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(5);
        instance = factory.newInstances()[0];
    }

    @Test
    public void testCommitConcurrently() {
        int count = 10000;
        String name = randomString();
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        ExecutorService executorServiceForCommit = Executors.newFixedThreadPool(5);
        HazelcastXAResource xaResource = instance.getXAResource();
        for (int i = 0; i < count; i++) {
            XATransactionRunnable runnable = new XATransactionRunnable(xaResource, name, executorServiceForCommit, i);
            executorService.execute(runnable);
        }
        IMap<Object, Object> map = instance.getMap(name);
        assertSizeEventually(count, map);
        executorService.shutdown();
        executorServiceForCommit.shutdown();
    }

    static class XATransactionRunnable implements Runnable {

        HazelcastXAResource xaResource;

        String name;

        ExecutorService executorServiceForCommit;

        int i;

        XATransactionRunnable(HazelcastXAResource xaResource, String name,
                                     ExecutorService executorServiceForCommit, int i) {
            this.xaResource = xaResource;
            this.name = name;
            this.executorServiceForCommit = executorServiceForCommit;
            this.i = i;
        }

        @Override
        public void run() {
            try {
                final Xid xid = createXid();
                xaResource.start(xid, XAResource.TMNOFLAGS);
                TransactionContext context = xaResource.getTransactionContext();
                TransactionalMap<Object, Object> map = context.getMap(name);
                map.put(i, i);
                xaResource.end(xid, XAResource.TMSUCCESS);
                executorServiceForCommit.execute(() -> {
                    try {
                        xaResource.commit(xid, true);
                    } catch (XAException e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
