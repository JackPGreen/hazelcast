/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.instance.impl;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.config.Config;
import com.hazelcast.config.NamespaceConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.namespace.impl.NamespaceAwareClassLoader;
import com.hazelcast.internal.namespace.impl.NamespaceThreadLocalContext;
import com.hazelcast.jet.impl.deployment.MapResourceClassLoader;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.IMap;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Test static namespace configuration, resource resolution and classloading end-to-end.
 */
public class NamespaceAwareClassLoaderIntegrationTest {

    private Config config;
    private ClassLoader nodeClassLoader;

    @Before
    public void setup() {
        config = new Config();
        config.setClassLoader(HazelcastInstance.class.getClassLoader());
    }

    @Test
    public void whenLoadClassKnownToParent_thenIsLoaded() throws Exception {
        nodeClassLoader = Node.getConfigClassloader(config);
        Class<?> klass = tryLoadClass("ns1", "com.hazelcast.core.HazelcastInstance");
        assertSame(HazelcastInstance.class, klass);
    }

    @Test
    public void whenLoadClassKnownToParent_thenIsLoadedWithNoNamespaceDefined() throws Exception {
        nodeClassLoader = Node.getConfigClassloader(config);
        Class<?> klass = tryLoadClass(null, "com.hazelcast.core.HazelcastInstance");
        assertSame(HazelcastInstance.class, klass);
    }

    @Test
    public void whenSimpleClassInNs_thenIsLoaded() throws Exception {
        config.addNamespaceConfig(new NamespaceConfig("ns1").addClass(toClass("usercodedeployment.ParentClass",
                new File("src/test/resources/usercodedeployment/ParentClass.class"))));
        nodeClassLoader = Node.getConfigClassloader(config);

        tryLoadClass("ns1", "usercodedeployment.ParentClass");
    }

    @Test
    public void whenSimpleClassInNs_thenIsNotLoadedWithNoNamespaceDefined() throws Exception {
        config.addNamespaceConfig(new NamespaceConfig("ns1").addClass(toClass("usercodedeployment.ParentClass",
                new File("src/test/resources/usercodedeployment/ParentClass.class"))));
        nodeClassLoader = Node.getConfigClassloader(config);

        assertThrows(ClassNotFoundException.class, () -> tryLoadClass(null, "usercodedeployment.ParentClass"));
    }

    @Test
    public void whenClassWithHierarchyInNs_thenIsLoaded() throws Exception {
        config.addNamespaceConfig(new NamespaceConfig("ns1")
                .addClass(toClass("usercodedeployment.ParentClass",
                        new File("src/test/resources/usercodedeployment/ParentClass.class")))
                .addClass(toClass("usercodedeployment.ChildClass",
                        new File("src/test/resources/usercodedeployment/ChildClass.class"))));
        nodeClassLoader = Node.getConfigClassloader(config);

        Class<?> childClass = tryLoadClass("ns1", "usercodedeployment.ChildClass");
        assertEquals("usercodedeployment.ParentClass", childClass.getSuperclass().getName());
        // assert child & parent are loaded by same classloader
        assertSame(childClass.getClassLoader(), childClass.getSuperclass().getClassLoader());
    }

    Class<?> tryLoadClass(String namespace, String className) throws Exception {
        if (namespace != null) {
            NamespaceThreadLocalContext.onStartNsAware(namespace);
        }
        try {
            return nodeClassLoader.loadClass(className);
        } finally {
            NamespaceThreadLocalContext.onCompleteNsAware(namespace);
        }
    }

    @Test
    public void testThatDoesNotBelongHere() throws Exception {
        config.addNamespaceConfig(new NamespaceConfig("ns1").addClass(toClass("usercodedeployment.IncrementingEntryProcessor",
                new File("src/test/resources/usercodedeployment/IncrementingEntryProcessor.class"))));
        config.getMapConfig("map-ns1").setNamespace("ns1");
        HazelcastInstance member1 = Hazelcast.newHazelcastInstance(config);
        HazelcastInstance member2 = Hazelcast.newHazelcastInstance(config);
        HazelcastInstance client = HazelcastClient.newHazelcastClient();
        IMap<Integer, Integer> map = client.getMap("map-ns1");
        // ensure the EntryProcessor can be deserialized on the member side
        for (int i = 0; i < 100; i++) {
            map.put(i, 1);
        }
        // use a different classloader with same config to instantiate the EntryProcessor
        NamespaceAwareClassLoader nsClassLoader = (NamespaceAwareClassLoader) Node.getConfigClassloader(config);
        @SuppressWarnings("unchecked")
        Class<? extends EntryProcessor<Integer, Integer, ?>> incrEPClass = (Class<? extends EntryProcessor<Integer, Integer, ?>>) nsClassLoader
                .loadClass("usercodedeployment.IncrementingEntryProcessor");
        EntryProcessor<Integer, Integer, ?> incrEp = incrEPClass.getDeclaredConstructor().newInstance();
        // invoke executeOnKey from client on all 100 keys
        for (int i = 0; i < 100; i++) {
            map.executeOnKey(i, incrEp);
            System.out.println(map.get(i));
        }
    }

    @Test
    public void whenLoadInnerClassKnownToParent_thenIsLoaded() throws Exception {
        // TODO You *could* load usercodedeployment.EntryProcessorWithAnonymousAndInner.Test from
        // /src/test/resources/usercodedeployment/EntryProcessorWithAnonymousAndInner.jar, but if the interface to load directly
        // from a .class file is going away, writing the test to do this is pointless
        throw new NotImplementedException();
    }

    /** TODO Implement this in a better way */
    private Class<?> toClass(String name, final File file) throws ClassNotFoundException {
        return new MapResourceClassLoader(getClass().getClassLoader(), () -> {
            try {
                return Map.of(name, FileUtils.readFileToByteArray(file));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, true).loadClass(name);
    }
}
