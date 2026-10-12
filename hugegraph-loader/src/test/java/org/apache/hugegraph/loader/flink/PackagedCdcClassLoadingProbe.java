/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.loader.flink;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Run against a built job JAR and Flink's lib directory, with no Maven classpath or database.
 */
public final class PackagedCdcClassLoadingProbe {

    private static final Logger LOG = Logger.getLogger(PackagedCdcClassLoadingProbe.class.getName());
    private static final String GUAVA = "org.apache.flink.shaded.guava";

    private PackagedCdcClassLoadingProbe() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected job JAR and Flink lib directory");
        }
        Path job = Path.of(args[0]).toRealPath();
        Path libraries = Path.of(args[1]).toRealPath();
        URL[] runtimeJars;
        try (Stream<Path> files = Files.list(libraries)) {
            runtimeJars = files.filter(path -> path.toString().endsWith(".jar")).sorted()
                               .map(PackagedCdcClassLoadingProbe::url).toArray(URL[]::new);
        }
        try (URLClassLoader runtime = new URLClassLoader(runtimeJars, ClassLoader.getPlatformClassLoader());
             URLClassLoader userCode = new URLClassLoader(new URL[]{url(job)}, runtime)) {
            Class<?> legacy = threadFactory(userCode, "31", "snapshot-splitting");
            if (!origin(legacy).equals(job)) {
                throw new AssertionError("CDC Guava31 must come from the packaged job");
            }
            Class<?> current = threadFactory(runtime, "33", "flink-runtime");
            if (origin(current).equals(job) || current.getClassLoader() != runtime) {
                throw new AssertionError("Flink Guava33 must remain owned by the runtime");
            }
            String[] coordinatorClasses = {
                    "org.apache.flink.cdc.connectors.mysql.source.assigners.MySqlSnapshotSplitAssigner",
                    "org.apache.flink.cdc.connectors.mysql.source.enumerator.MySqlSourceEnumerator"
            };
            for (String name : coordinatorClasses) {
                Class<?> coordinator = Class.forName(name, false, userCode);
                coordinator.getDeclaredConstructors();
                coordinator.getDeclaredMethods();
                if (!origin(coordinator).equals(job)) {
                    throw new AssertionError("CDC coordinator must come from the packaged job");
                }
                LOG.info(name + " links from " + origin(coordinator));
            }
            Class<?> source = Class.forName("org.apache.flink.api.connector.source.Source", false, userCode);
            if (source.getClassLoader() != runtime) {
                throw new AssertionError("The job must use the runtime's public Flink source API");
            }
            LOG.info("Guava31=" + origin(legacy) + ", Guava33=" + origin(current) +
                     ", FlinkSource=" + origin(source));
        }
    }

    private static Class<?> threadFactory(ClassLoader loader, String version, String name) throws Exception {
        Class<?> type = Class.forName(GUAVA + version + ".com.google.common.util.concurrent.ThreadFactoryBuilder",
                                     true, loader);
        Object builder = type.getConstructor().newInstance();
        type.getMethod("setNameFormat", String.class).invoke(builder, name);
        ThreadFactory factory = (ThreadFactory) type.getMethod("build").invoke(builder);
        Thread thread = factory.newThread(() -> { });
        if (!thread.getName().equals(name)) {
            throw new AssertionError("CDC's thread factory contract failed");
        }
        // Creating the factory exercises CDC's failing dependency; never start the thread.
        return type;
    }

    private static Path origin(Class<?> type) throws Exception {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
    }

    private static URL url(Path path) {
        try {
            return path.toUri().toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid classpath entry", e);
        }
    }
}
