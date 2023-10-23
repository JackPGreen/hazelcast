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

import com.google.common.base.CaseFormat;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.test.TestHazelcastFactory;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NamespaceConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.namespace.impl.NamespaceAwareClassLoader;
import com.hazelcast.internal.namespace.impl.NamespaceThreadLocalContext;
import com.hazelcast.internal.nio.IOUtil;
import com.hazelcast.internal.util.OsHelper;
import com.hazelcast.jet.impl.deployment.MapResourceClassLoader;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.IMap;
import com.hazelcast.map.MapLoader;
import com.hazelcast.test.Accessors;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.SlowTest;
import com.hazelcast.test.starter.MavenInterface;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hazelcast.jet.impl.JobRepository.classKeyName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/** Test static namespace configuration, resource resolution and classloading end-to-end */
@RunWith(HazelcastSerialClassRunner.class)
@Category(SlowTest.class)
public class NamespaceAwareClassLoaderIntegrationTest extends HazelcastTestSupport {
    private static Path classRoot;
    protected static MapResourceClassLoader mapResourceClassLoader;
    private static Artifact h2V202Artifact;

    protected Config config;
    private ClassLoader nodeClassLoader;

    @BeforeClass
    public static void setUpClass() throws IOException {
        // TODO Remove?
        System.setProperty(MapResourceClassLoader.DEBUG_OUTPUT_PROPERTY, "true");
        classRoot = Paths.get("src/test/class");
        mapResourceClassLoader = generateMapResourceClassLoaderForDirectory(classRoot);
        h2V202Artifact = new DefaultArtifact("com.h2database", "h2", null, "2.0.202");
    }

    @AfterClass
    public static void cleanUpClass() {
        // TODO Remove?
        System.clearProperty(MapResourceClassLoader.DEBUG_OUTPUT_PROPERTY);
    }

    @Before
    public void setUp() {
        config = new Config();
        config.setClassLoader(HazelcastInstance.class.getClassLoader());
    }

    /**
     * Asserts a basic user workflow:
     * <ol>
     * <li>Add classes to the {@link NamespaceConfig}
     * <li>Assert that those classes aren't accessible by default
     * <li>Configure the a {@link HazelcastInstance} with isolated {@link IMap}s using those classes
     * <li>Assert that those classes are isolated - even with overlapping names, the correct class is used
     * <ol>
     *
     * @see <a href="https://hazelcast.atlassian.net/browse/HZ-3301">HZ-3301 - Test case for Milestone 1 use cases</a>
     */
    @Test
    public void testMilestone1() throws Exception {
        // "I can statically configure a namespace with a java class that gets resolved at runtime"
        for (CaseValueProcessor processor : CaseValueProcessor.values()) {
            processor.addNamespaceToConfig(config);
        }

        HazelcastInstance hazelcastInstance = createHazelcastInstance(config);
        nodeClassLoader = Accessors.getNode(hazelcastInstance).getConfigClassLoader();

        // "I can run a customer entry processor and configure an IMap in that namespace"
        // "to execute that entry processor on that IMap"
        // "I can configure N > 1 namespaces with simple Java class resources of same name and different behavior"
        for (CaseValueProcessor processor : CaseValueProcessor.values()) {
            processor.createExecuteAssertOnMap(this, hazelcastInstance);
        }

        // "IMaps configured in the respective namespaces will correctly load and execute the respective EntryProcessor defined
        // in their namespace, without class name clashes."
        for (CaseValueProcessor processor : CaseValueProcessor.values()) {
            processor.assertEntryUpdated();
        }
    }

    /**
     * Asserts a basic user workflow:
     * <ol>
     * <li>Creates a {@link NamespaceConfig} referencing a {@link MapLoader} {@code .class} and it's database dependency - which
     * *aren't* on the classpath
     * <li>Create a map, configuring the {@link NamespaceConfig} and {@link MapLoader}
     * <li>Assert the {@link MapLoader} has loaded data via the database dependency via some dummy call
     * <ol>
     *
     * @see <a href="https://hazelcast.atlassian.net/browse/HZ-3357">HZ-3357 - Test case for Milestone 1 dependencies use
     *      cases</a>
     */
    @Test
    public void testMilestone1Dependencies() throws Exception {
        // "As a Java developer, I can define a MapLoader with JDBC driver dependency in a namespace and IMap configured with
        // that namespace will correctly instantiate and use my MapLoader."

        String mapName = randomMapName();
        String className = "usercodedeployment.DerbyUpperCaseStringMapLoader";

        Assert.assertThrows("The test class should not be already accessible", ClassNotFoundException.class,
                () -> Class.forName(className));

        // Add the latest Derby version that supports Java 11 (newer versions require Java 17)
        NamespaceConfig namespace = new NamespaceConfig("ns1").addClass(mapResourceClassLoader.loadClass(className))
                .addJar(MavenInterface.locateArtifact(new DefaultArtifact("org.apache.derby", "derby", null, "10.15.2.0"))
                        .toUri().toURL())
                .addJar(MavenInterface.locateArtifact(new DefaultArtifact("org.apache.derby", "derbyshared", null, "10.15.2.0"))
                        .toUri().toURL());

        config.getNamespacesConfig().addNamespaceConfig(namespace);
        config.getMapConfig(mapName).setNamespace(namespace.getName()).getMapStoreConfig().setEnabled(true)
                .setClassName(className);

        HazelcastInstance hazelcastInstance = createHazelcastInstance(config);
        nodeClassLoader = Accessors.getNode(hazelcastInstance).getConfigClassLoader();

        String mapped = executeMapLoader(hazelcastInstance, mapName);
        assertNotNull("Was the MapStore executed?", mapped);
        assertEquals(getClass().getSimpleName().toUpperCase(), mapped);
    }

    /**
     * Asserts a basic user workflow:
     * <ol>
     * <li>Creates a {@link NamespaceConfig} referencing a {@link MapLoader} {@code .class} and it's database dependency - which
     * is a *different* one than on classpath
     * <li>Create a map, configuring the {@link NamespaceConfig} and {@link MapLoader}
     * <li>Assert the {@link MapLoader} has used the {@link NamespaceConfig} database dependency version
     * <ol>
     *
     * @see <a href="https://hazelcast.atlassian.net/browse/HZ-3357">HZ-3357 - Test case for Milestone 1 dependencies use
     *      cases</a>
     */
    @Test
    public void testMilestone1DependenciesIsolation() throws Exception {
        // "Isolation against Hazelcast member classpath: even when Hazelcast member classpath includes a clashing version of my
        // JDBC driver, my preferred JDBC driver version that is configured in namespace resources is used by my MapLoader."

        String mapName = randomMapName();
        String className = "usercodedeployment.H2WithDriverManagerBuildVersionMapLoader";

        Assert.assertThrows("The test class should not be already accessible", ClassNotFoundException.class,
                () -> Class.forName(className));

        // Deliberately use an older version
        NamespaceConfig namespace = new NamespaceConfig("ns1").addClass(mapResourceClassLoader.loadClass(className))
                .addJar(MavenInterface.locateArtifact(h2V202Artifact).toUri().toURL());
        config.getNamespacesConfig().addNamespaceConfig(namespace);

        config.getMapConfig(mapName).setNamespace(namespace.getName()).getMapStoreConfig().setEnabled(true)
                .setClassName(className);

        HazelcastInstance hazelcastInstance = createHazelcastInstance(config);
        nodeClassLoader = Accessors.getNode(hazelcastInstance).getConfigClassLoader();

        String namespaceH2Version = executeMapLoader(hazelcastInstance, mapName);

        assertNotNull("Was the MapStore executed?", namespaceH2Version);
        assertEquals("Unexpected version of H2 found in namespace", h2V202Artifact.getVersion(), namespaceH2Version);
        assertNotEquals("Namespaces dependencies do not appear to be isolated", org.h2.engine.Constants.VERSION,
                namespaceH2Version);
    }

    @Test
    public void testThatDoesNotBelongHere() throws Exception {
        int nodeCount = 2;
        TestHazelcastFactory factory = new TestHazelcastFactory(nodeCount);

        try {
            config.getNamespacesConfig().addNamespaceConfig(new NamespaceConfig("ns1")
                    .addClass(mapResourceClassLoader.loadClass("usercodedeployment.IncrementingEntryProcessor")));
            config.getMapConfig("map-ns1").setNamespace("ns1");

            // Get the first instance
            HazelcastInstance hazelcastInstance = factory.newHazelcastInstance(config);

            // Construct the rest but don't keep a reference
            for (int i = 1; i < nodeCount; i++) {
                factory.newHazelcastInstance(config);
            }

            HazelcastInstance client = factory.newHazelcastClient();
            IMap<Integer, Integer> map = client.getMap("map-ns1");
            // ensure the EntryProcessor can be deserialized on the member side
            for (int i = 0; i < 100; i++) {
                map.put(i, 1);
            }
            // use a different classloader with same config to instantiate the EntryProcessor
            ClassLoader nsClassLoader = Accessors.getNode(hazelcastInstance).getConfigClassLoader();
            @SuppressWarnings("unchecked")
            Class<? extends EntryProcessor<Integer, Integer, ?>> incrEPClass = (Class<? extends EntryProcessor<Integer, Integer, ?>>) nsClassLoader
                    .loadClass("usercodedeployment.IncrementingEntryProcessor");
            EntryProcessor<Integer, Integer, ?> incrEp = incrEPClass.getDeclaredConstructor().newInstance();
            // invoke executeOnKey from client on all 100 keys
            for (int i = 0; i < 100; i++) {
                map.executeOnKey(i, incrEp);
                System.out.println(map.get(i));
            }
        } finally {
            factory.terminateAll();
        }
    }

    /**
     * @see <a href="https://hazelcast.atlassian.net/browse/HZ-3390">HZ-3390 - Support ServiceLoader in
     *      NamespaceAwareClassLoader</a>
     */
    @Test
    public void testServiceLoader() throws Exception {
        // Class + Map Name
        Pair<String, String> driverManager = Pair.of("usercodedeployment.H2WithDriverManagerBuildVersionMapLoader",
                randomMapName());
        Pair<String, String> dataSource = Pair.of("usercodedeployment.H2WithDataSourceBuildVersionMapLoader", randomMapName());

        Pair<String, String>[] classes = new Pair[] {driverManager, dataSource};

        NamespaceConfig namespace = new NamespaceConfig("ns1")
                .addJar(MavenInterface.locateArtifact(h2V202Artifact).toUri().toURL());
        config.getNamespacesConfig().addNamespaceConfig(namespace);

        for (Pair<String, String> clazz : classes) {
            namespace.addClass(mapResourceClassLoader.loadClass(clazz.getLeft()));
            config.getMapConfig(clazz.getRight()).setNamespace(namespace.getName()).getMapStoreConfig().setEnabled(true)
                    .setClassName(clazz.getLeft());
        }

        HazelcastInstance hazelcastInstance = createHazelcastInstance(config);
        nodeClassLoader = Accessors.getNode(hazelcastInstance).getConfigClassLoader();

        assertEquals("Fixture setup of JDBC with explicit driver declaration", h2V202Artifact.getVersion(),
                executeMapLoader(hazelcastInstance, dataSource.getRight()));

        assertEquals("JDBC generally is working, but Driver Manager isn't - suggests Service Loader issue",
                h2V202Artifact.getVersion(), executeMapLoader(hazelcastInstance, driverManager.getRight()));
    }

    @Test
    public void testMemberToMemberDeserialization_Simple_2Node() throws ReflectiveOperationException {
        testMemberToMemberDeserialization(2,
                "usercodedeployment.IncrementingStringEntryProcessor",
                "usercodedeployment.IncrementingStringEntryProcessor");
    }

    @Test
    public void testMemberToMemberDeserialization_Simple_5Node() throws ReflectiveOperationException {
        testMemberToMemberDeserialization(5,
                "usercodedeployment.IncrementingStringEntryProcessor",
                "usercodedeployment.IncrementingStringEntryProcessor");
    }

    @Test
    public void testMemberToMemberDeserialization_Complex_2Node() throws ReflectiveOperationException {
        testMemberToMemberDeserialization(2,
                "usercodedeployment.ComplexStringEntryProcessor",
                "usercodedeployment.ComplexStringEntryProcessor",
                "usercodedeployment.ComplexProcessor");
    }

    @Test
    public void testMemberToMemberDeserialization_Complex_5Node() throws ReflectiveOperationException {
        testMemberToMemberDeserialization(5,
                "usercodedeployment.ComplexStringEntryProcessor",
                "usercodedeployment.ComplexStringEntryProcessor",
                "usercodedeployment.ComplexProcessor");
    }

    // TODO use test parameters for nodeCount
    private void testMemberToMemberDeserialization(int nodeCount, String entryProcessClassName,
                                                               String... resourceClassNames) throws ReflectiveOperationException {
        assertGreaterOrEquals("nodeCount", nodeCount, 2);
        Assert.assertThrows("The entry processor class should not be already accessible: " + entryProcessClassName,
                ClassNotFoundException.class, () -> Class.forName(entryProcessClassName));

        TestHazelcastFactory factory = new TestHazelcastFactory(nodeCount);
        final String mapName = randomMapName();
        configureSimpleNodeConfig(mapName, "my-ep-ns", resourceClassNames);
        HazelcastInstance[] instances = factory.newInstances(config, nodeCount);

        // Create map on the cluster as normal, populate with data for each partition
        IMap<String, Integer> map = instances[0].getMap(mapName);
        for (int k = 0; k < 10; k++) {
            int j = 1;
            for (HazelcastInstance instance : instances) {
                map.put(generateKeyOwnedBy(instance), j++);
            }
        }

        // Assert ownership distribution
        Set<String> member1Keys = map.localKeySet();
        assertEquals(10, member1Keys.size());

        for (int k = 1; k < instances.length; k++) {
            HazelcastInstance instance = instances[k];
            IMap<String, Integer> memberMap = instance.getMap(mapName);
            assertEquals(10, memberMap.localKeySet().size());
        }

        // Create a client that only communicates with member1 (unisocket)
        HazelcastInstance client = createUnisocketClient(factory);

        // Execute processor on keys owned by other members
        IMap<String, Integer> clientMap = client.getMap(mapName);
        EntryProcessor entryProcessor = (EntryProcessor) mapResourceClassLoader.loadClass(entryProcessClassName)
                                                                               .getConstructor().newInstance();
        for (int k = 0; k < instances.length; k++) {
            HazelcastInstance instance = instances[k];
            String key = (String) instance.getMap(mapName).localKeySet().iterator().next();
            clientMap.executeOnKey(key, entryProcessor);

            // Assert processor completed successfully
            int expectedOutput = k + 2;
            assertTrueEventually(() -> assertEquals(expectedOutput, clientMap.get(key).intValue()));
        }
        factory.shutdownAll();
    }

    // TODO: Fails due to `namespace` not serialized in dynamic config yet - should pass once added
    @Test
    public void testDynamicConfigMapLoaderDeserialization_2Node() throws ReflectiveOperationException {
        testMemberToMemberMLDeserialization(2,
                "usercodedeployment.KeyBecomesValueMapLoader",
                "usercodedeployment.KeyBecomesValueMapLoader");
    }

    // TODO: Fails due to `namespace` not serialized in dynamic config yet - should pass once added
    @Test
    public void testDynamicConfigMapLoaderDeserialization_5Node() throws ReflectiveOperationException {
        testMemberToMemberMLDeserialization(5,
                "usercodedeployment.KeyBecomesValueMapLoader",
                "usercodedeployment.KeyBecomesValueMapLoader");
    }

    // TODO use test parameters for nodeCount
    private void testMemberToMemberMLDeserialization(int nodeCount, String mapLoaderClassName,
                                                               String... resourceClassNames) throws ReflectiveOperationException {
        assertGreaterOrEquals("nodeCount", nodeCount, 2);
        Assert.assertThrows("The MapLoader class should not be already accessible: " + mapLoaderClassName,
                ClassNotFoundException.class, () -> Class.forName(mapLoaderClassName));

        final TestHazelcastFactory factory = new TestHazelcastFactory(nodeCount);
        final String mapName = randomMapName();
        final String namespace = "my-ml-ns";
        configureSimpleNodeConfig(null, namespace, resourceClassNames);
        HazelcastInstance[] instances = factory.newInstances(config, nodeCount);

        // Create a client that only communicates with member1 (unisocket)
        HazelcastInstance client = createUnisocketClient(factory);

        // Dynamically add a new MapConfig with a MapLoader that needs to be loaded
        MapConfig mapConfig = new MapConfig(mapName).setNamespace(namespace);
        mapConfig.getMapStoreConfig().setEnabled(true).setClassName(mapLoaderClassName);
        client.getConfig().addMapConfig(mapConfig);

        // Trigger map loader on all members and assert values are correct
        for (int k = 0; k < instances.length; k++) {
            HazelcastInstance instance = instances[k];
            String result = executeMapLoader(instance, mapName);
            assertEquals(getClass().getSimpleName(), result);
        }
        factory.shutdownAll();
    }

    private HazelcastInstance createUnisocketClient(TestHazelcastFactory factory) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701");
        clientConfig.getNetworkConfig().setSmartRouting(false);
        return factory.newHazelcastClient(clientConfig);
    }

    private void configureSimpleNodeConfig(String mapName, String namespaceId, String... classNames) throws ClassNotFoundException {
        for (String className : classNames) {
            Assert.assertThrows("The test class should not be already accessible: " + className,
                    ClassNotFoundException.class, () -> Class.forName(className));
        }

        NamespaceConfig namespace = new NamespaceConfig(namespaceId);
        for (String name : classNames) {
            namespace.addClass(mapResourceClassLoader.loadClass(name));
        }
        config.getNamespacesConfig().addNamespaceConfig(namespace);
        config.getNetworkConfig().setPort(5701);
        if (mapName != null) {
            config.getMapConfig(mapName)
                  .setNamespace(namespace.getName());
        }
    }

    /** Find & load all .class files in the scope of this test */
    private static MapResourceClassLoader generateMapResourceClassLoaderForDirectory(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root.resolve("usercodedeployment"))) {
            final Map<String, byte[]> classNameToContent = stream
                    .filter(path -> FilenameUtils.isExtension(path.getFileName().toString(), "class"))
                    .collect(Collectors.toMap(path -> correctResourcePath(root, path), path -> {
                        try {
                            return IOUtil.compress(Files.readAllBytes(path));
                        } catch (final IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }));

            return new MapResourceClassLoader(NamespaceAwareClassLoaderIntegrationTest.class.getClassLoader(),
                    () -> classNameToContent, true);
        }
    }

    private static String correctResourcePath(Path root, Path path) {
        String classKeyName = classKeyName(root.relativize(path).toString());
        if (OsHelper.isWindows()) {
            classKeyName = classKeyName.replace('\\', '/');
        }
        return classKeyName;
    }

    Class<?> tryLoadClass(String namespace, String className) throws Exception {
        if (namespace != null) {
            NamespaceThreadLocalContext.onStartNsAware(namespace);
        }
        try {
            return nodeClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException(
                    MessageFormat.format("\"{0}\" class not found in \"{1}\" namespace", className, namespace), e);
        } finally {
            if (namespace != null) {
                NamespaceThreadLocalContext.onCompleteNsAware(namespace);
            }
        }
    }

    private String executeMapLoader(HazelcastInstance hazelcastInstance, String mapName) {
        return hazelcastInstance.<String, String>getMap(mapName).get(getClass().getSimpleName());
    }

    /**
     * References to {@link EntryProcessor}s - not on the classpath - that modify the case of a {@link String} value in a map
     */
    private enum CaseValueProcessor {
        UPPER_CASE_VALUE_ENTRY_PROCESSOR(String::toUpperCase), LOWER_CASE_VALUE_ENTRY_PROCESSOR(String::toLowerCase);

        /** Use the same class name to assert isolation */
        private static final String className = "usercodedeployment.ModifyCaseValueEntryProcessor";
        private static final Object KEY = Void.TYPE;
        private static final String VALUE = "VaLuE";

        /** The operation we expect the {@link EntryProcessor} to perform - for validation purposes */
        private final UnaryOperator<String> expectedOperation;
        private final NamespaceConfig namespace;
        private final String mapName = randomMapName();
        private IMap<Object, String> map;

        /** @param expectedOperation {@link #expectedOperation} */
        CaseValueProcessor(UnaryOperator<String> expectedOperation) {
            this.expectedOperation = expectedOperation;

            try {
                namespace = new NamespaceConfig(toString()).addClass(
                        generateMapResourceClassLoaderForDirectory(classRoot.resolve("usercodedeployment").resolve(toString()))
                                .loadClass(className));
            } catch (ClassNotFoundException | IOException e) {
                throw new ExceptionInInitializerError(e);
            }

            Assert.assertThrows("The test class should not be already accessible", ClassNotFoundException.class,
                    () -> Class.forName(className));
        }

        private void addNamespaceToConfig(Config config) {
            config.getNamespacesConfig().addNamespaceConfig(namespace);
            config.getMapConfig(mapName).setNamespace(toString());
        }

        private void createExecuteAssertOnMap(NamespaceAwareClassLoaderIntegrationTest instance,
                HazelcastInstance hazelcastInstance) throws Exception {
            // Create a map
            map = hazelcastInstance.getMap(mapName);
            map.put(KEY, VALUE);

            // Execute the EntryProcessor
            Class<? extends EntryProcessor<Object, String, String>> clazz = (Class<? extends EntryProcessor<Object, String, String>>) instance
                    .tryLoadClass(toString(), className);
            map.executeOnKey(Void.TYPE, clazz.getDeclaredConstructor().newInstance());

            assertEntryUpdated();
        }

        private void assertEntryUpdated() {
            assertEquals(expectedOperation.apply(VALUE), map.get(KEY));
        }

        @Override
        public String toString() {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
        }
    }
}
