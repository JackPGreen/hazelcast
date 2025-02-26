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

package com.hazelcast.client.collections;

import com.hazelcast.client.test.TestHazelcastFactory;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.collection.IList;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class ClientListTest extends HazelcastTestSupport {

    private TestHazelcastFactory hazelcastFactory;
    private IList<String> list;

    @Before
    public void setup() {
        hazelcastFactory = new TestHazelcastFactory();
        hazelcastFactory.newHazelcastInstance();
        HazelcastInstance client = hazelcastFactory.newHazelcastClient();
        list = client.getList(randomString());
    }

    @After
    public void tearDown() {
        hazelcastFactory.shutdownAll();
    }

    @Test
    public void testAddAll() {
        List<String> l = new ArrayList<>();
        l.add("item1");
        l.add("item2");

        assertTrue(list.addAll(l));
        assertEquals(2, list.size());

        assertTrue(list.addAll(1, l));
        assertEquals(4, list.size());

        assertEquals("item1", list.get(0));
        assertEquals("item1", list.get(1));
        assertEquals("item2", list.get(2));
        assertEquals("item2", list.get(3));
    }

    @Test
    public void testAddSetRemove() {
        assertTrue(list.add("item1"));
        assertTrue(list.add("item2"));
        list.add(0, "item3");
        assertEquals(3, list.size());
        String element = list.set(2, "item4");
        assertEquals("item2", element);

        assertEquals(3, list.size());
        assertEquals("item3", list.get(0));
        assertEquals("item1", list.get(1));
        assertEquals("item4", list.get(2));

        assertFalse(list.remove("item2"));
        assertTrue(list.remove("item3"));

        element = list.remove(1);
        assertEquals("item4", element);

        assertEquals(1, list.size());
        assertEquals("item1", list.get(0));
    }

    @Test
    public void testIndexOf() {
        assertTrue(list.add("item1"));
        assertTrue(list.add("item2"));
        assertTrue(list.add("item1"));
        assertTrue(list.add("item4"));

        assertEquals(-1, list.indexOf("item5"));
        assertEquals(0, list.indexOf("item1"));

        assertEquals(-1, list.lastIndexOf("item6"));
        assertEquals(2, list.lastIndexOf("item1"));
    }

    @Test
    public void testIterator() {
        assertTrue(list.add("item1"));
        assertTrue(list.add("item2"));
        assertTrue(list.add("item1"));
        assertTrue(list.add("item4"));

        Iterator iter = list.iterator();
        assertEquals("item1", iter.next());
        assertEquals("item2", iter.next());
        assertEquals("item1", iter.next());
        assertEquals("item4", iter.next());
        assertFalse(iter.hasNext());

        ListIterator listIterator = list.listIterator(2);
        assertEquals("item1", listIterator.next());
        assertEquals("item4", listIterator.next());
        assertFalse(listIterator.hasNext());

        List l = list.subList(1, 3);
        assertEquals(2, l.size());
        assertEquals("item2", l.get(0));
        assertEquals("item1", l.get(1));
    }

    @Test
    public void testSpliterator() {
        assertTrue(list.add("item1"));
        assertTrue(list.add("item2"));
        assertTrue(list.add("item3"));
        assertTrue(list.add("item4"));

        Spliterator spliterator = list.spliterator();

        ArrayList recorder = new ArrayList(list.size());
        Consumer consumer = recorder::add;

        // tryAdvance.
        assertTrue(spliterator.tryAdvance(consumer));
        assertEquals(list.get(0), recorder.get(0));

        // forEachRemaining.
        spliterator.forEachRemaining(consumer);
        assertCollection(list, recorder);

        // There should be no more elements remaining in this spliterator.
        assertFalse(spliterator.tryAdvance(consumer));
        spliterator.forEachRemaining(item -> fail());

    }

    @Test
    public void testContains() {
        assertTrue(list.add("item1"));
        assertTrue(list.add("item2"));
        assertTrue(list.add("item1"));
        assertTrue(list.add("item4"));

        assertNotContains(list, "item3");
        assertContains(list, "item2");

        List<String> l = new ArrayList<>();
        l.add("item4");
        l.add("item3");

        assertNotContainsAll(list, l);
        assertTrue(list.add("item3"));
        assertContainsAll(list, l);
    }

    @Test
    public void removeRetainAll() {
        assertTrue(list.add("item1"));
        assertTrue(list.add("item2"));
        assertTrue(list.add("item1"));
        assertTrue(list.add("item4"));

        List<String> l = new ArrayList<>();
        l.add("item4");
        l.add("item3");

        assertTrue(list.removeAll(l));
        assertEquals(3, list.size());
        assertFalse(list.removeAll(l));
        assertEquals(3, list.size());

        l.clear();
        l.add("item1");
        l.add("item2");
        assertFalse(list.retainAll(l));
        assertEquals(3, list.size());

        l.clear();
        assertTrue(list.retainAll(l));
        assertEquals(0, list.size());
    }

    @Test
    public void testListener() throws Exception {

        final CountDownLatch addLatch = new CountDownLatch(6);
        final CountDownLatch removeLatch = new CountDownLatch(1);

        ItemListener<String> listener = new ItemListener<>() {

            public void itemAdded(ItemEvent<String> itemEvent) {
                addLatch.countDown();
            }

            public void itemRemoved(ItemEvent<String> item) {
                removeLatch.countDown();
            }
        };
        UUID registrationId = list.addItemListener(listener, true);

        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                list.add("item" + i);
            }
            list.add("done");
            list.remove("done");
        }).start();
        assertOpenEventually(addLatch);
        assertOpenEventually(removeLatch);
        list.removeItemListener(registrationId);
    }

    @Test
    public void testIsEmpty_whenEmpty() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testIsEmpty_whenNotEmpty() {
        list.add("item");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
    }
}
