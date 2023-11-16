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

package com.hazelcast.jet.impl.deployment;

import com.hazelcast.internal.nio.IOUtil;
import com.hazelcast.jet.impl.util.ReflectionUtils;
import com.hazelcast.jet.impl.util.Util;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.InflaterInputStream;

import static com.hazelcast.internal.util.StringUtil.isNullOrEmpty;
import static com.hazelcast.jet.impl.JobRepository.classKeyName;
import static com.hazelcast.jet.impl.JobRepository.fileKeyName;
import static com.hazelcast.jet.impl.util.ReflectionUtils.toClassResourceId;

/**
 * Class loader that can be customized with:
 * <ul>
 *     <li>A resources supplier {@code Supplier<? extends Map<String, byte[]>>}.</li>
 *     <li>Choice of whether it looks up classes and resources in child-first order (so this ClassLoader's resources are
 *     first search, then, if not found, the parent ClassLoader is queried). If not child-first, then the common parent-first
 *     hierarchical ClassLoader model is followed.</li>
 * </ul>
 * Resources in the {@code <? extends Map<String, byte[]>>} supplied by the resource supplier are expected to follow
 * these conventions:
 *  <ul>
 *      <li>the {@code byte[]} value is the {@link IOUtil#compress(byte[]) deflated} payload of the resource</li>
 *      <li>key of classes is composed with a {@code "c."} prefix, followed by the classname as file path and the
 *      {@code ".class"} suffix. Assuming {@code className} is the binary name of the class, the key is formatted
 *      as {@code classKeyName(toClassResourceId(className))}. See
 *      {@link com.hazelcast.jet.impl.JobRepository#classKeyName(String)} and
 *      {@link com.hazelcast.jet.impl.util.ReflectionUtils#toClassResourceId(String)}.</li>
 *      <li>key of other classpath resources if composed of the {@code "f."} prefix and the path of the resource.
 *      See also {@link com.hazelcast.jet.impl.JobRepository#fileKeyName(String)}.</li>
 *  </ul>
 */
public class MapResourceClassLoader extends JetDelegatingClassLoader {
    public static final String DEBUG_OUTPUT_PROPERTY = "hazelcast.classloading.debug";

    static final String PROTOCOL = "map-resource";

    // TODO Shouldn't this be a logging param?
    private static final boolean DEBUG_OUTPUT = Boolean.getBoolean(DEBUG_OUTPUT_PROPERTY);

    // TODO a more effecient way to do this
    protected final Map<String, byte[]> extraResources = new HashMap<>();

    protected final Supplier<? extends Map<String, byte[]>> resourcesSupplier;
    /**
     * When {@code true}, if the requested class/resource is not found in this ClassLoader's resources, then parent
     * is queried. Otherwise, only resources in this ClassLoader are searched.
     */
    protected final boolean childFirst;
    protected volatile boolean isShutdown;

    private final ILogger logger = Logger.getLogger(getClass());
    private final @Nullable String namespace;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    // Jet only constructor
    MapResourceClassLoader(ClassLoader parent,
                                     @Nonnull Supplier<? extends Map<String, byte[]>> resourcesSupplier,
                                     boolean childFirst) {
        super(parent);
        this.namespace = null;
        this.resourcesSupplier = Util.memoizeConcurrent(resourcesSupplier);
        this.childFirst = childFirst;
    }

    // UCD Namespaces oriented constructor
    public MapResourceClassLoader(@Nonnull String namespace, ClassLoader parent,
                                     @Nonnull Supplier<? extends Map<String, byte[]>> resourcesSupplier,
                                     boolean childFirst) {
        super("ucd-namespace", parent);
        this.namespace = namespace;
        this.resourcesSupplier = Util.memoizeConcurrent(resourcesSupplier);
        this.childFirst = childFirst;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!childFirst) {
            return super.loadClass(name, resolve);
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> klass = findLoadedClass(name);
            // first lookup class in own resources
            try {
                if (klass == null) {
                    klass = findClass(name);
                }
            } catch (ClassNotFoundException ignored) {
                if (DEBUG_OUTPUT) {
                    logger.finest(ignored);
                }
            }
            if (klass == null && getParent() != null) {
                klass = getParent().loadClass(name);
            }
            if (klass == null) {
                throw new ClassNotFoundException(name);
            }
            if (resolve) {
                resolveClass(klass);
            }
            return klass;
        }
    }

    /** Allow direct addition of a class to ensure that this classloader can load it in addition */
    public void addExtraClass(Class<?> clazz) throws IOException {
        extraResources.put(ReflectionUtils.toClassResourceId(clazz),
                ReflectionUtils.getClassContent(clazz.getName(), clazz.getClassLoader()));
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (isNullOrEmpty(name)) {
            return null;
        }
        byte[] classBytes = resourceBytes(toClassResourceId(name));
        if (classBytes == null) {
            throw newClassNotFoundException(name);
        }
        definePackage(name);
        return defineClass(name, classBytes, 0, classBytes.length);
    }

    @Override
    public void shutdown() {
        isShutdown = true;
    }

    @Nullable
    @Override
    public URL getResource(@Nonnull String name) {
        Objects.requireNonNull(name);
        if (!childFirst) {
            return super.getResource(name);
        }
        URL res = findResource(name);
        if (res == null) {
            res = super.getResource(name);
        }
        return res;
    }

    @Override
    protected URL findResource(String name) {
        if (checkShutdown(name) || isNullOrEmpty(name)) {
            return null;
        }

        if (!getResourceMap().containsKey(classKeyName(name)) && !getResourceMap().containsKey(fileKeyName(name))) {
            return null;
        }

        try {
            return new URL(PROTOCOL, null, -1, name, new MapResourceURLStreamHandler());
        } catch (MalformedURLException e) {
            // this should never happen with custom URLStreamHandler
            throw new RuntimeException(e);
        }
    }

    private Map<String, byte[]> getResourceMap() {
        return Stream.of(resourcesSupplier.get(), extraResources).flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        URL foundResource = findResource(name);
        return foundResource == null
                ? Collections.emptyEnumeration()
                : Collections.enumeration(Collections.singleton(foundResource));
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    // argument is used in overridden implementation
    @SuppressWarnings("java:S1172")
    boolean checkShutdown(@SuppressWarnings("unused") String resource) {
        return isShutdown;
    }

    @Nullable
    InputStream resourceStream(String name) {
        if (checkShutdown(name)) {
            return null;
        }
        byte[] classData = getBytes(name);
        if (classData == null) {
            return null;
        }
        return new InflaterInputStream(new ByteArrayInputStream(classData));
    }

    @Nullable
    byte[] resourceBytes(String name) {
        if (checkShutdown(name)) {
            return null;
        }
        byte[] classData = getBytes(name);
        if (classData == null) {
            return null;
        }
        return IOUtil.decompress(classData);
    }

    @Nullable
    private byte[] getBytes(String name) {
        byte[] classData = getResourceMap().get(classKeyName(name));
        if (classData == null) {
            classData = getResourceMap().get(fileKeyName(name));
            if (classData == null) {
                return null;
            }
        }
        return classData;
    }

    ClassNotFoundException newClassNotFoundException(String name) {
        if (DEBUG_OUTPUT) {
            String message = "For name " + name + " no resource could be identified. List of resources:\n"
                    + getResourceMap().keySet();
            return new ClassNotFoundException(message);
        }
        return new ClassNotFoundException(name);
    }

    /**
     * Defines the package if it is not already defined for the given class
     * name.
     *
     * @param className the class name
     */
    void definePackage(String className) {
        if (isNullOrEmpty(className)) {
            return;
        }
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return;
        }
        String packageName = className.substring(0, lastDotIndex);
        if (getDefinedPackage(packageName) != null) {
            return;
        }
        try {
            definePackage(packageName, null, null, null, null, null, null, null);
        } catch (IllegalArgumentException ignored) {
            // ignore
        }
    }

    protected final class MapResourceURLStreamHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new MapResourceURLConnection(u);
        }
    }

    private final class MapResourceURLConnection extends URLConnection {
        MapResourceURLConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return resourceStream(url.getFile());
        }
    }
}
