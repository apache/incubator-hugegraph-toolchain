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

package org.apache.hugegraph.loader.test.unit;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import org.apache.hugegraph.loader.executor.LoadOptions;
import org.junit.Test;

import org.apache.hugegraph.testutil.Assert;

public class LoadOptionsTest {

    @Test
    public void testConnectionPoolAutoAdjustWithDefaultBatchThreads() throws Exception {
        int cpus = readStaticInt(LoadOptions.class, "CPUS");
        LoadOptions options = new LoadOptions();

        Assert.assertEquals(cpus * 4, options.maxConnections);
        Assert.assertEquals(cpus * 2, options.maxConnectionsPerRoute);
    }

    @Test
    public void testConnectionPoolAutoAdjustWithCustomBatchThreads() throws Exception {
        int cpus = readStaticInt(LoadOptions.class, "CPUS");
        int defaultMaxConn = readStaticInt(LoadOptions.class, "DEFAULT_MAX_CONNECTIONS");
        int defaultMaxConnPerRoute = readStaticInt(LoadOptions.class,
                                                  "DEFAULT_MAX_CONNECTIONS_PER_ROUTE");
        LoadOptions options = new LoadOptions();
        options.batchInsertThreads = 20;

        CapturingAppender appender = attachAppender();
        try {
            invokeAdjustConnectionPool(options);
        } finally {
            detachAppender(appender);
        }

        int expectedMaxConn = defaultMaxConn;
        int expectedMaxConnPerRoute = defaultMaxConnPerRoute;
        if (defaultMaxConn == cpus * 4 && defaultMaxConn < 80) {
            expectedMaxConn = 80;
        }
        if (defaultMaxConnPerRoute == cpus * 2 && defaultMaxConnPerRoute < 40) {
            expectedMaxConnPerRoute = 40;
        }

        Assert.assertEquals(expectedMaxConn, options.maxConnections);
        Assert.assertEquals(expectedMaxConnPerRoute, options.maxConnectionsPerRoute);
        if (expectedMaxConn == 80 || expectedMaxConnPerRoute == 40) {
            Assert.assertTrue(appender.contains("Auto adjusted max-conn"));
        }
    }

    @Test
    public void testConnectionPoolNoAdjustWithCustomMaxConn() throws Exception {
        LoadOptions options = new LoadOptions();
        options.batchInsertThreads = 20;
        options.maxConnections = 100;
        options.maxConnectionsPerRoute = 50;


        CapturingAppender appender = attachAppender();
        try {
            invokeAdjustConnectionPool(options);
        } finally {
            detachAppender(appender);
        }

        Assert.assertEquals(100, options.maxConnections);
        Assert.assertEquals(50, options.maxConnectionsPerRoute);
        Assert.assertFalse(appender.contains("Auto adjusted max-conn"));
    }

    @Test
    public void testParseThreadsMinValue() {
        LoadOptions.PositiveValidator validator =
                new LoadOptions.PositiveValidator();

        validator.validate("--parser-threads", "1");

        Assert.assertTrue(validateFails(validator, "--parser-threads", "0"));
        Assert.assertTrue(validateFails(validator, "--parser-threads", "-1"));
    }

    @Test
    public void testParseThreadsDefaultValue() throws Exception {
        int cpus = readStaticInt(LoadOptions.class, "CPUS");
        LoadOptions options = new LoadOptions();
        Assert.assertEquals(Math.max(2, cpus / 2), options.parseThreads);
    }

    @Test
    public void testDeprecatedParallelCountParameter() throws Exception {
        File mapping = createTempMapping();
        String[] args = new String[]{
                "-f", mapping.getPath(),
                "-g", "g",
                "-h", "localhost",
                "--parallel-count", "4"
        };

        CapturingAppender appender = attachAppender();
        try {
            LoadOptions options = LoadOptions.parseOptions(args);
            Assert.assertEquals(4, options.parseThreads);
            Assert.assertTrue(appender.contains("deprecated"));
        } finally {
            detachAppender(appender);
            mapping.delete();
        }
    }

    private static int readStaticInt(Class<?> type, String name)
                                     throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(null);
    }

    private static void invokeAdjustConnectionPool(LoadOptions options)
                                                   throws Exception {
        Method method = LoadOptions.class
                                   .getDeclaredMethod("adjustConnectionPoolIfDefault",
                                                      LoadOptions.class);
        method.setAccessible(true);
        method.invoke(null, options);
    }

    private static boolean validateFails(LoadOptions.PositiveValidator validator,
                                         String name, String value) {
        try {
            validator.validate(name, value);
            return false;
        } catch (Exception ignored) {
            return true;
        }
    }

    private static File createTempMapping() throws Exception {
        File file = File.createTempFile("load-options-", ".json", new File("."));
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("{\"version\":\"2.0\",\"structs\":[]}");
        }
        return file;
    }

    private static CapturingAppender attachAppender() {
        Logger logger = Logger.getLogger(LoadOptions.class.getName());
        CapturingAppender appender = new CapturingAppender();
        appender.setThreshold(Level.INFO);
        logger.addAppender(appender);
        return appender;
    }

    private static void detachAppender(CapturingAppender appender) {
        if (appender == null) {
            return;
        }
        Logger logger = Logger.getLogger(LoadOptions.class.getName());
        logger.removeAppender(appender);
    }

    private static final class CapturingAppender extends AppenderSkeleton {

        private final StringBuilder buffer = new StringBuilder();

        @Override
        protected void append(LoggingEvent event) {
            if (event == null || event.getRenderedMessage() == null) {
                return;
            }
            buffer.append(event.getRenderedMessage()).append('\n');
        }

        boolean contains(String text) {
            return this.buffer.toString().contains(text);
        }

        @Override
        public void close() {
            // No-op.
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    }

}
