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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.apache.hugegraph.driver.GraphsManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.HugeClientBuilder;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.exception.InvalidResponseException;
import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.loader.builder.SchemaCache;
import org.apache.hugegraph.loader.constant.ElemType;
import org.apache.hugegraph.loader.exception.InsertException;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.exception.ParseException;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.failure.FailLogger;
import org.apache.hugegraph.loader.failure.FailWriter;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.LoadMapping;
import org.apache.hugegraph.loader.metrics.LoadSummary;
import org.apache.hugegraph.loader.progress.LoadProgress;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.loader.source.graph.GraphSource;
import org.apache.hugegraph.structure.LoadingModeOwner;
import org.apache.hugegraph.structure.constant.GraphMode;
import org.apache.hugegraph.util.JsonUtil;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.net.httpserver.HttpServer;

import okhttp3.OkHttpClient;

public class LoadContextTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testOfflineContextDoesNotCreateClientsOrSaveProgress() throws IOException {
        LoadOptions options = new LoadOptions();
        options.host = null;
        options.direct = true;
        options.file = this.folder.newFile("offline.json").getAbsolutePath();
        SchemaCache schemaCache = new SchemaCache(Collections.emptyList(),
                                                  Collections.emptyList(),
                                                  Collections.emptyList());
        LoadContext context = LoadContext.forOffline(options, schemaCache);
        Assert.assertSame(schemaCache, context.schemaCache());
        Assert.assertNull(context.client());
        Assert.assertNull(context.indirectClient());
        Assert.assertNotNull(context.filterGroup());
        context.close();
        context.close();
        Assert.assertTrue(context.closed());
        Assert.assertFalse(new File(LoadProgress.format(options, context.timestamp())).exists());
    }

    @Test
    public void testReadLimitStopPreservesSuccess() throws Exception {
        LoadContext context = this.readLimitContext();
        context.stopLoadingAtReadLimit();
        Assert.assertTrue(context.stopped());
        Assert.assertTrue(context.stoppedAtReadLimit());
        Assert.assertTrue(context.noError());
        context.throwIfFailed();
        context.stopLoading();
        Assert.assertTrue(context.stopped());
        Assert.assertFalse(context.stoppedAtReadLimit());
        Assert.assertTrue(context.noError());
    }

    @Test
    public void testErrorsOverrideReadLimitStop() throws Exception {
        LoadContext context = this.readLimitContext();
        context.stopLoadingAtReadLimit();
        context.occurredError();
        Assert.assertTrue(context.stopped());
        Assert.assertFalse(context.stoppedAtReadLimit());
        Assert.assertFalse(context.noError());
        context.stopLoadingAtReadLimit();
        Assert.assertFalse(context.stoppedAtReadLimit());
    }

    @Test
    public void testReadLimitDoesNotOverrideEarlierErrorOrStop() throws Exception {
        LoadContext context = this.readLimitContext();
        context.occurredError();
        Assert.assertFalse(context.stopped());
        context.stopLoadingAtReadLimit();
        Assert.assertTrue(context.stopped());
        Assert.assertFalse(context.stoppedAtReadLimit());
        Assert.assertFalse(context.noError());

        LoadContext stopped = this.readLimitContext();
        stopped.stopLoading();
        stopped.stopLoadingAtReadLimit();
        Assert.assertFalse(stopped.stoppedAtReadLimit());
    }

    private LoadContext readLimitContext() throws IOException {
        LoadOptions options = new LoadOptions();
        options.file = this.folder.newFile().getAbsolutePath();
        return LoadContext.forOffline(options, new SchemaCache(Collections.emptyList(),
                                                               Collections.emptyList(),
                                                               Collections.emptyList()));
    }

    @Test
    public void testCloseDistinctClientsOnce() throws Exception {
        RecordingClient client = allocate(RecordingClient.class);
        RecordingClient indirect = allocate(RecordingClient.class);
        LoadContext context = this.context(client, indirect);
        context.close();
        context.close();
        Assert.assertEquals(1, client.closeCalls);
        Assert.assertEquals(1, indirect.closeCalls);
        Assert.assertTrue(context.closed());
    }

    @Test
    public void testCloseSharedClientOnce() throws Exception {
        RecordingClient client = allocate(RecordingClient.class);
        LoadContext context = this.context(client, client);
        context.close();
        context.close();
        Assert.assertEquals(1, client.closeCalls);
    }

    @Test
    public void testCloseIndirectClientWhenPrimaryCloseFails() throws Exception {
        RecordingClient client = allocate(RecordingClient.class);
        RecordingClient indirect = allocate(RecordingClient.class);
        client.closeFailure = "Primary close failed";
        LoadContext context = this.context(client, indirect);
        try {
            context.close();
            Assert.fail("Expected primary close failure");
        } catch (IllegalStateException expected) {
            Assert.assertEquals("Primary close failed", expected.getMessage());
            Assert.assertEquals(0, expected.getSuppressed().length);
        }
        assertClosedWithoutRetrying(context, client, indirect);
    }

    @Test
    public void testCloseRemainsClosedWhenSecondaryCloseFails() throws Exception {
        RecordingClient client = allocate(RecordingClient.class);
        RecordingClient indirect = allocate(RecordingClient.class);
        indirect.closeFailure = "Indirect close failed";
        LoadContext context = this.context(client, indirect);
        try {
            context.close();
            Assert.fail("Expected indirect close failure");
        } catch (IllegalStateException expected) {
            Assert.assertEquals("Indirect close failed", expected.getMessage());
            Assert.assertEquals(0, expected.getSuppressed().length);
        }
        assertClosedWithoutRetrying(context, client, indirect);
    }

    @Test
    public void testPreservePrimaryFailureWhenBothClosesFail() throws Exception {
        RecordingClient client = allocate(RecordingClient.class);
        RecordingClient indirect = allocate(RecordingClient.class);
        client.closeFailure = "Primary close failed";
        indirect.closeFailure = "Indirect close failed";
        LoadContext context = this.context(client, indirect);
        try {
            context.close();
            Assert.fail("Expected primary close failure");
        } catch (IllegalStateException expected) {
            Assert.assertEquals("Primary close failed", expected.getMessage());
            Assert.assertEquals(1, expected.getSuppressed().length);
            Assert.assertEquals("Indirect close failed", expected.getSuppressed()[0].getMessage());
        }
        assertClosedWithoutRetrying(context, client, indirect);
    }

    @Test
    public void testLoggerFailureClosesClientsWithoutSavingProgress() throws Exception {
        RecordingClient client = allocate(RecordingClient.class);
        RecordingClient indirect = allocate(RecordingClient.class);
        client.closeFailure = "Primary close failed";
        indirect.closeFailure = "Indirect close failed";
        LoadContext context = this.context(client, indirect);
        FileSource source = new FileSource();
        source.header(new String[]{"name"});
        InputStruct struct = new InputStruct(null, null);
        struct.id("broken");
        struct.input(source);
        FailLogger logger = allocate(FailLogger.class);
        set(logger, "struct", struct);
        set(logger, "file", this.folder.newFile("broken.error"));
        // A directory at the header's file path makes the actual header write fail.
        this.folder.newFolder("broken.header");
        HashMap<String, FailLogger> loggers = new HashMap<>();
        loggers.put("broken", logger);
        set(context, "loggers", loggers);
        try {
            context.close();
            Assert.fail("Expected failure logger close failure");
        } catch (LoadException expected) {
            Assert.assertTrue(expected.getMessage().contains("Failed to write header"));
            Assert.assertTrue(expected.getCause() instanceof IOException);
            Assert.assertEquals(2, expected.getSuppressed().length);
            Assert.assertEquals("Primary close failed", expected.getSuppressed()[0].getMessage());
            Assert.assertEquals("Indirect close failed", expected.getSuppressed()[1].getMessage());
        }
        Assert.assertTrue(context.closed());
        context.close();
        Assert.assertEquals(1, client.closeCalls);
        Assert.assertEquals(1, indirect.closeCalls);
        Assert.assertEquals(0L, context.newProgress().vertexLoaded());
        Assert.assertEquals(0L, context.newProgress().edgeLoaded());
        Assert.assertFalse(new File(LoadProgress.format(context.options(), "test")).exists());
    }

    @Test
    public void testProgressFailureStillClosesBothClients() throws Exception {
        RecordingClient client = allocate(RecordingClient.class);
        RecordingClient indirect = allocate(RecordingClient.class);
        client.closeFailure = "Primary close failed";
        indirect.closeFailure = "Indirect close failed";
        LoadContext context = this.context(client, indirect);
        context.options().file = "invalid-mapping-suffix";
        try {
            context.close();
            Assert.fail("Expected invalid mapping filename");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("mapping description file name"));
            Assert.assertEquals(2, expected.getSuppressed().length);
            Assert.assertEquals("Primary close failed", expected.getSuppressed()[0].getMessage());
            Assert.assertEquals("Indirect close failed", expected.getSuppressed()[1].getMessage());
        }
        assertClosedWithoutRetrying(context, client, indirect);
    }

    @Test
    public void testProgressWriteFailureIsReportedAndClosesClients() throws Exception {
        RecordingClient client = allocate(RecordingClient.class);
        RecordingClient indirect = allocate(RecordingClient.class);
        LoadContext context = this.context(client, indirect);
        File progress = new File(LoadProgress.format(context.options(), "test"));
        Assert.assertTrue(progress.mkdirs());
        try {
            context.close();
            Assert.fail("Expected progress write failure");
        } catch (LoadException expected) {
            Assert.assertTrue(expected.getMessage().contains("Failed to write load progress"));
            Assert.assertTrue(expected.getCause() instanceof IOException);
        }
        assertClosedWithoutRetrying(context, client, indirect);
    }

    @Test
    public void testCloseAttemptsEveryFailureLoggerAndPreservesErrors() throws Exception {
        RecordingClient client = allocate(RecordingClient.class);
        RecordingClient indirect = allocate(RecordingClient.class);
        client.closeFailure = "Primary close failed";
        indirect.closeFailure = "Indirect close failed";
        LoadContext context = this.context(client, indirect);
        InputStruct struct = new InputStruct(null, null);
        struct.id("healthy");
        struct.input(new FileSource());
        FailLogger healthy = new FailLogger(context, struct);
        healthy.write(new InsertException("recover-me", "insert failed"));
        File healthyFile = new File(context.options().file.replace(".json", ""),
                                    "failure-data/healthy.error");
        Assert.assertEquals(0L, healthyFile.length());
        LinkedHashMap<String, FailLogger> loggers = new LinkedHashMap<>();
        loggers.put("first", this.loggerWithCloseFailure("first"));
        loggers.put("second", this.loggerWithCloseFailure("second"));
        loggers.put("healthy", healthy);
        set(context, "loggers", loggers);
        try {
            context.close();
            Assert.fail("Expected failure logger close errors");
        } catch (LoadException expected) {
            Assert.assertTrue("Healthy failure data must be flushed",
                              Files.readAllLines(healthyFile.toPath()).contains("recover-me"));
            Assert.assertEquals("first close failed", expected.getCause().getMessage());
            Assert.assertEquals(3, expected.getSuppressed().length);
            Assert.assertEquals("second close failed", expected.getSuppressed()[0].getCause().getMessage());
            Assert.assertEquals("Primary close failed", expected.getSuppressed()[1].getMessage());
            Assert.assertEquals("Indirect close failed", expected.getSuppressed()[2].getMessage());
        }
        Assert.assertTrue(Files.readAllLines(healthyFile.toPath()).contains("recover-me"));
        Assert.assertEquals(1, client.closeCalls);
        Assert.assertEquals(1, indirect.closeCalls);
        Assert.assertFalse(context.noError());
        Assert.assertFalse(new File(LoadProgress.format(context.options(), "test")).exists());
    }

    private FailLogger loggerWithCloseFailure(String name) throws Exception {
        File file = this.folder.newFile(name + ".error");
        BufferedWriter buffer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file) {
            @Override
            public void close() throws IOException {
                super.close();
                throw new IOException(name + " close failed");
            }
        }, StandardCharsets.UTF_8));
        FailWriter writer = allocate(FailWriter.class);
        set(writer, "file", file);
        set(writer, "writer", buffer);
        writer.write(new InsertException(name + "-raw", "insert failed"));
        InputStruct struct = new InputStruct(null, null);
        struct.input(new FileSource());
        FailLogger logger = allocate(FailLogger.class);
        set(logger, "struct", struct);
        set(logger, "file", file);
        set(logger, "writer", writer);
        return logger;
    }

    @Test
    public void testFailureLogFlushErrorPreventsProgress() throws Exception {
        RecordingClient client = allocate(RecordingClient.class);
        LoadContext context = this.context(client, client);
        FileSource source = new FileSource();
        InputStruct struct = new InputStruct(null, null);
        struct.input(source);
        File file = this.folder.newFile("flush.error");
        FailWriter writer = allocate(FailWriter.class);
        set(writer, "file", file);
        set(writer, "writer", new BufferedWriter(new StringWriter()) {
            @Override
            public void close() throws IOException {
                throw new IOException("Failure data flush failed");
            }
        });
        FailLogger logger = allocate(FailLogger.class);
        set(logger, "struct", struct);
        set(logger, "file", file);
        set(logger, "writer", writer);
        HashMap<String, FailLogger> loggers = new HashMap<>();
        loggers.put("flush", logger);
        set(context, "loggers", loggers);
        try {
            context.close();
            Assert.fail("Expected failure data flush error");
        } catch (LoadException expected) {
            Assert.assertTrue(expected.getCause() instanceof IOException);
        }
        Assert.assertFalse(context.noError());
        Assert.assertEquals(1, client.closeCalls);
        Assert.assertFalse(new File(LoadProgress.format(context.options(), "test")).exists());
    }

    @Test
    public void testParseFailureKeepsOriginalExceptionAndPreventsProgress() throws Exception {
        RecordingClient client = allocate(RecordingClient.class);
        RecordingClient indirect = allocate(RecordingClient.class);
        LoadContext context = this.context(client, indirect);
        ParseException failure = new ParseException("invalid-row", "original parse error");
        context.failLoading(failure);
        context.failLoading(new LoadException("later failure"));
        try {
            context.throwIfFailed();
            Assert.fail("Expected original parse failure");
        } catch (ParseException expected) {
            Assert.assertSame(failure, expected);
        }
        try {
            context.close();
            Assert.fail("Expected original parse failure during close");
        } catch (ParseException expected) {
            Assert.assertSame(failure, expected);
            Assert.assertEquals("original parse error", expected.getMessage());
        }
        Assert.assertFalse(context.noError());
        Assert.assertTrue(context.stopped());
        Assert.assertTrue(context.closed());
        Assert.assertEquals(1, client.closeCalls);
        Assert.assertEquals(1, indirect.closeCalls);
        Assert.assertEquals(0L, context.newProgress().vertexLoaded());
        Assert.assertFalse(new File(LoadProgress.format(context.options(), "test")).exists());
    }

    @Test
    public void testUnrecoverableWorkerErrorPreventsProgressAndClosesClients() throws Exception {
        RecordingClient client = allocate(RecordingClient.class);
        RecordingClient indirect = allocate(RecordingClient.class);
        LoadContext context = this.context(client, indirect);
        IOException failure = new IOException("failure data unavailable");
        context.failLoading(failure);
        try {
            context.close();
            Assert.fail("Expected worker failure");
        } catch (LoadException expected) {
            Assert.assertSame(failure, expected.getCause());
        }
        Assert.assertFalse(context.noError());
        Assert.assertTrue(context.closed());
        Assert.assertEquals(1, client.closeCalls);
        Assert.assertEquals(1, indirect.closeCalls);
        Assert.assertEquals(0L, context.newProgress().vertexLoaded());
        Assert.assertFalse(new File(LoadProgress.format(context.options(), "test")).exists());
    }

    @Test
    public void testMixedSourceStartupDoesNotClearAnotherLoadingMode() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            LoadContext context = this.context(client, client);
            InputStruct graph = new InputStruct(null, null);
            graph.input(new GraphSource());
            InputStruct file = new InputStruct(null, null);
            file.input(new FileSource());
            HugeGraphLoader loader = loader(context, graph, file);
            try {
                loader.load();
                Assert.fail("Expected mixed Graph and File startup failure");
            } catch (LoadException expected) {
                Assert.assertEquals("All inputs must be of Graph Type", expected.getMessage());
            }
            try {
                loader.shutdown();
                Assert.fail("Expected the original startup failure during close");
            } catch (LoadException expected) {
                Assert.assertEquals("All inputs must be of Graph Type", expected.getMessage());
            }
            loader.shutdown();
            Assert.assertTrue(context.closed());
            Assert.assertEquals(GraphMode.LOADING, server.mode);
            Assert.assertEquals(0, server.modeReads);
            Assert.assertEquals(0, server.modeWrites);
        }
    }

    @Test
    public void testFailedLoadingModeSetDoesNotClearAnotherLoadingMode() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            LoadContext context = this.context(client, client);
            server.failure = "Mode change denied";
            try {
                context.setLoadingMode();
                Assert.fail("Expected mode set failure");
            } catch (ServerException expected) {
                Assert.assertEquals("Mode change denied", expected.getMessage());
            }
            server.failure = null;
            context.unsetLoadingMode();
            Assert.assertEquals(GraphMode.LOADING, server.mode);
            Assert.assertEquals(0, server.modeReads);
            Assert.assertEquals(1, server.modeWrites);
        }
    }

    @Test
    public void testUnsupportedLoadingModeDoesNotAcquireCleanup() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            LoadContext context = this.context(client, client);
            server.failure = "Can not deserialize value of type GraphMode";
            context.setLoadingMode();
            server.failure = null;
            context.unsetLoadingMode();
            Assert.assertEquals(GraphMode.LOADING, server.mode);
            Assert.assertEquals(0, server.modeReads);
            Assert.assertEquals(1, server.modeWrites);
        }
    }

    @Test
    public void testSuccessfulLoadingModeCleanupRunsOnce() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            LoadContext context = this.context(client, client);
            server.mode = GraphMode.NONE;
            context.setLoadingMode();
            Assert.assertEquals(GraphMode.LOADING, server.mode);
            context.unsetLoadingMode();
            Assert.assertEquals(GraphMode.NONE, server.mode);
            // Another Loader starts after this context has finished cleanup.
            server.mode = GraphMode.LOADING;
            context.unsetLoadingMode();
            Assert.assertEquals(GraphMode.LOADING, server.mode);
            Assert.assertEquals(1, server.modeReads);
            Assert.assertEquals(2, server.modeWrites);
        }
    }

    @Test
    public void testLoadingCleanupPreservesOtherMode() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            LoadContext context = this.context(client, client);
            context.setLoadingMode();
            server.mode = GraphMode.RESTORING;
            context.unsetLoadingMode();
            Assert.assertEquals(GraphMode.RESTORING, server.mode);
            server.mode = GraphMode.LOADING;
            context.unsetLoadingMode();
            Assert.assertEquals(GraphMode.LOADING, server.mode);
            Assert.assertEquals(1, server.modeReads);
            Assert.assertEquals(1, server.modeWrites);
        }
    }

    @Test
    public void testRestoreModeDoesNotAcquireLoadingCleanup() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            LoadContext context = this.context(client, client);
            context.setRestoreMode();
            Assert.assertEquals(GraphMode.RESTORING, server.mode);
            server.mode = GraphMode.LOADING;
            context.unsetLoadingMode();
            Assert.assertEquals(GraphMode.LOADING, server.mode);
            Assert.assertEquals(0, server.modeReads);
            Assert.assertEquals(1, server.modeWrites);
        }
    }

    @Test
    public void testClonedContextDoesNotInheritLoadingCleanup() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            LoadContext context = this.context(client, client);
            context.setLoadingMode();
            LoadContext cloned = (LoadContext) context.clone();
            cloned.unsetLoadingMode();
            Assert.assertEquals(GraphMode.LOADING, server.mode);
            Assert.assertEquals(0, server.modeReads);
            Assert.assertEquals(1, server.modeWrites);
            context.unsetLoadingMode();
            Assert.assertEquals(GraphMode.NONE, server.mode);
        }
    }

    @Test
    public void testCleanupReadFailureCanBeRetried() throws Exception {
        this.assertCleanupFailureCanBeRetried(true);
    }

    @Test
    public void testCleanupWriteFailureCanBeRetried() throws Exception {
        this.assertCleanupFailureCanBeRetried(false);
    }

    private void assertCleanupFailureCanBeRetried(boolean readFailure) throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            LoadContext context = this.context(client, client);
            context.setLoadingMode();
            if (readFailure) {
                server.readFailure = "Cleanup read denied";
            } else {
                server.failure = "Cleanup write denied";
            }
            try {
                context.unsetLoadingMode();
                Assert.fail("Expected cleanup failure");
            } catch (LoadException expected) {
                Assert.assertTrue(expected.getCause() instanceof ServerException);
            }
            Assert.assertEquals(GraphMode.LOADING, server.mode);
            server.readFailure = null;
            server.failure = null;
            context.unsetLoadingMode();
            Assert.assertEquals(GraphMode.NONE, server.mode);
            Assert.assertEquals(2, server.modeReads);
            Assert.assertEquals(readFailure ? 2 : 3, server.modeWrites);
            // A completed retry must not acquire cleanup of a subsequent import.
            server.mode = GraphMode.LOADING;
            context.unsetLoadingMode();
            Assert.assertEquals(GraphMode.LOADING, server.mode);
            Assert.assertEquals(2, server.modeReads);
            Assert.assertEquals(readFailure ? 2 : 3, server.modeWrites);
        }
    }

    @Test
    public void testUnconfirmedModeSetPreservesModeAndStartupFailure() throws Exception {
        Logger logger = Logger.getLogger(LoadContext.class.getName());
        CapturingAppender appender = new CapturingAppender();
        appender.setThreshold(Level.ERROR);
        logger.addAppender(appender);
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            LoadContext context = this.context(client, client);
            InputStruct file = new InputStruct(null, null);
            file.input(new FileSource());
            HugeGraphLoader loader = loader(context, file);
            server.mode = GraphMode.NONE;
            server.dropModeResponse = true;
            IOException failure = null;
            try {
                loader.load();
                Assert.fail("Expected failure after the mode change was applied");
            } catch (RuntimeException expected) {
                Assert.assertTrue(expected.getCause() instanceof IOException);
                failure = (IOException) expected.getCause();
            }
            Assert.assertNotNull(failure);
            Assert.assertTrue(appender.contains("graph 'hugegraph'"));
            Assert.assertTrue(appender.contains("may still be in LOADING mode"));
            Assert.assertTrue(appender.contains("no imports are active"));
            Assert.assertTrue(appender.contains("manually reset it to NONE"));
            Assert.assertEquals(GraphMode.LOADING, server.mode);
            int writes = server.modeWrites;
            Assert.assertTrue(writes > 0);
            server.dropModeResponse = false;
            try {
                loader.shutdown();
                Assert.fail("Expected the original unconfirmed mode change failure");
            } catch (LoadException expected) {
                Assert.assertSame(failure, expected.getCause());
            }
            loader.shutdown();
            Assert.assertTrue(context.closed());
            Assert.assertEquals(GraphMode.LOADING, server.mode);
            Assert.assertEquals(0, server.modeReads);
            Assert.assertEquals(writes, server.modeWrites);
        } finally {
            logger.removeAppender(appender);
        }
    }

    @Test
    public void testShutdownPreservesOwnerRequestWithoutLeakingToken() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            server.enableOwnership();
            server.dropOwnerReply = true;
            LoadContext context = this.context(client, client);
            InputStruct input = new InputStruct(null, null);
            input.input(new FileSource());
            HugeGraphLoader loader = loader(context, input);
            RuntimeException loading = (RuntimeException) Assert.assertThrows(RuntimeException.class, loader::load);
            Map<String, Object> acquired = new HashMap<>(server.lastOwner);
            String identity = acquired.get("instance") + "/" + acquired.get("epoch");
            LoadException shutdown = (LoadException) Assert.assertThrows(LoadException.class, loader::shutdown);
            Assert.assertTrue(shutdown.getMessage().contains("Failed to release LOADING owner " + identity));
            Assert.assertFalse(shutdown.getMessage().contains((String) acquired.get("token")));
            Assert.assertEquals(1, shutdown.getSuppressed().length);
            Assert.assertSame(loading.getCause(), shutdown.getSuppressed()[0].getCause());
            Assert.assertTrue(shutdown.getCause() instanceof IOException);
            Assert.assertEquals("release", server.lastOwner.remove("action"));
            acquired.remove("action");
            Assert.assertEquals(acquired, server.lastOwner);
            Assert.assertTrue(context.closed());
            int writes = server.ownerWrites;
            loader.shutdown();
            Assert.assertEquals(writes, server.ownerWrites);
            Assert.assertEquals(0, server.modeWrites);
        }
    }

    @Test
    public void testShutdownPreservesReleaseFailureWhenLoggerCloseFails() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            server.enableOwnership();
            LoadContext context = this.context(client, client);
            context.setLoadingMode();
            String identity = server.lastOwner.get("instance") + "/" + server.lastOwner.get("epoch");
            FileSource source = new FileSource();
            source.header(new String[]{"name"});
            InputStruct input = new InputStruct(null, null);
            input.input(source);
            input.id("broken");
            FailLogger logger = allocate(FailLogger.class);
            set(logger, "struct", input);
            set(logger, "file", this.folder.newFile("broken.error"));
            this.folder.newFolder("broken.header");
            set(context, "loggers", Map.of("broken", logger));
            server.dropOwnerReply = true;
            LoadException shutdown = (LoadException) Assert.assertThrows(LoadException.class,
                    loader(context, input)::shutdown);
            Assert.assertTrue(shutdown.getMessage().contains("Failed to release LOADING owner " + identity));
            Assert.assertEquals(1, shutdown.getSuppressed().length);
            Assert.assertTrue(shutdown.getSuppressed()[0].getMessage().contains("Failed to write header"));
            Assert.assertTrue(context.closed());
        }
    }

    @Test
    public void testShutdownReportsCloseFailureAfterSuccessfulRelease() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            server.enableOwnership();
            LoadContext context = this.context(client, client);
            context.setLoadingMode();
            RuntimeException loading = new IllegalStateException("original loading failure");
            context.failLoading(loading);
            HugeGraphLoader loader = loader(context);
            Assert.assertSame(loading, Assert.assertThrows(RuntimeException.class, loader::shutdown));
            Assert.assertEquals(0, loading.getSuppressed().length);
            Assert.assertTrue(context.closed());
            loader.shutdown();
            Assert.assertEquals(2, server.ownerWrites);
        }
    }

    @Test
    public void testCapabilityFailuresDoNotFallBackToLegacyMode() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            for (int status : new int[]{403, 404, 500}) {
                server.ownerStatus = status;
                server.ownerCapability = "{\"exception\":\"class jakarta.ws.rs.NotFoundException\"," +
                                         "\"message\":\"Graph does not exist\"}";
                LoadContext context = this.context(client, client);
                Assert.assertThrows(ServerException.class, context::setLoadingMode);
                context.unsetLoadingMode();
            }
            server.ownerStatus = 200;
            server.ownerCapability = "{}";
            LoadContext invalid = this.context(client, client);
            Assert.assertThrows(InvalidResponseException.class, invalid::setLoadingMode);
            server.dropCapabilityReply = true;
            Assert.assertThrows(IOException.class, invalid::setLoadingMode);
            invalid.unsetLoadingMode();
            Assert.assertEquals(0, server.modeWrites);
            Assert.assertEquals(0, server.ownerWrites);
        }
    }

    @Test
    public void testCapabilityRevisionKeepsOrdinaryOwnerRequestsCompatible() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            server.enableOwnership();
            Map<String, Object> capability = JsonUtil.fromJson(server.ownerCapability, Map.class);
            capability.put("revision", 7L);
            server.ownerCapability = JsonUtil.toJson(capability);
            LoadingModeOwner owner = client.graphs().newLoadingOwner("hugegraph");
            client.graphs().acquireLoadingOwner("hugegraph", owner);
            Assert.assertEquals(Set.of("action", "instance", "epoch", "token"), server.lastOwner.keySet());
            Assert.assertEquals(capability.get("instance"), server.lastOwner.get("instance"));
            Assert.assertEquals(capability.get("epoch"), server.lastOwner.get("epoch"));
            Map<String, Object> acquired = new HashMap<>(server.lastOwner);
            client.graphs().releaseLoadingOwner("hugegraph", owner);
            acquired.put("action", "release");
            Assert.assertEquals(acquired, server.lastOwner);
        }
    }

    @Test
    public void testManualRestoringModeRejectionDoesNotFallBack() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            server.enableOwnership();
            server.mode = GraphMode.RESTORING;
            server.ownerWriteStatus = 400;
            server.ownerFailure = "Cannot acquire loading ownership of manually set graph mode RESTORING";
            LoadContext context = this.context(client, client);
            ServerException failure = (ServerException) Assert.assertThrows(ServerException.class,
                    context::setLoadingMode);
            Assert.assertEquals(server.ownerFailure, failure.getMessage());
            Assert.assertEquals(GraphMode.RESTORING, server.mode);
            Assert.assertEquals(0, server.modeWrites);
            server.ownerWriteStatus = 200;
            server.ownerFailure = null;
            context.unsetLoadingMode();
            Assert.assertEquals("release", server.lastOwner.get("action"));
            Assert.assertEquals(0, server.modeWrites);
        }
    }

    @Test
    public void testOwnerRecoveryLogDoesNotExposeToken() throws Exception {
        Logger logger = Logger.getLogger(LoadContext.class.getName());
        CapturingAppender appender = new CapturingAppender();
        Level original = logger.getLevel();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            server.enableOwnership();
            LoadContext context = this.context(client, client);
            context.setLoadingMode();
            String identity = server.lastOwner.get("instance") + "/" + server.lastOwner.get("epoch");
            Assert.assertTrue(appender.contains("Preparing LOADING owner " + identity));
            Assert.assertTrue(appender.contains("Server loading-owner recovery procedure"));
            Assert.assertFalse(appender.contains((String) server.lastOwner.get("token")));
            context.unsetLoadingMode();
        } finally {
            logger.removeAppender(appender);
            logger.setLevel(original);
        }
    }

    @Test
    public void testOwnerCleanupRetriesSameTupleAndNeverUsesEnumCleanup() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            server.enableOwnership();
            LoadContext context = this.context(client, client);
            context.setLoadingMode();
            Map<String, Object> acquired = new HashMap<>(server.lastOwner);
            ((LoadContext) context.clone()).unsetLoadingMode();
            server.dropOwnerReply = true;
            Assert.assertThrows(LoadException.class, context::unsetLoadingMode);
            server.dropOwnerReply = false;
            context.unsetLoadingMode();
            Assert.assertEquals("release", server.lastOwner.remove("action"));
            acquired.remove("action");
            Assert.assertEquals(acquired, server.lastOwner);
            int writes = server.ownerWrites;
            context.unsetLoadingMode();
            Assert.assertEquals(writes, server.ownerWrites);
            Assert.assertThrows(LoadException.class, context::setLoadingMode);
            Assert.assertEquals(GraphMode.LOADING, server.mode);
            Assert.assertEquals(0, server.modeWrites);
        }
    }

    @Test
    public void testLostOrDeniedAcquireKeepsTupleForCleanupWithoutFallback() throws Exception {
        try (ModeServer server = new ModeServer(); HugeClient client = server.client()) {
            server.enableOwnership();
            for (boolean lostReply : new boolean[]{false, true}) {
                LoadContext context = this.context(client, client);
                server.dropOwnerReply = lostReply;
                server.ownerWriteStatus = lostReply ? 200 : 403;
                Assert.assertThrows(lostReply ? IOException.class : ServerException.class, context::setLoadingMode);
                Map<String, Object> acquired = new HashMap<>(server.lastOwner);
                server.dropOwnerReply = false;
                server.ownerWriteStatus = 200;
                context.unsetLoadingMode();
                Assert.assertEquals("release", server.lastOwner.remove("action"));
                acquired.remove("action");
                Assert.assertEquals(acquired, server.lastOwner);
            }
            Assert.assertEquals(0, server.modeWrites);
        }
    }

    private static final class CapturingAppender extends AppenderSkeleton {

        private final StringBuilder messages = new StringBuilder();

        @Override
        protected void append(LoggingEvent event) {
            this.messages.append(event.getRenderedMessage()).append('\n');
        }

        private boolean contains(String text) {
            return this.messages.toString().contains(text);
        }

        @Override
        public void close() {
            // No resources owned by this appender.
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    }

    private static HugeGraphLoader loader(LoadContext context, InputStruct... inputs) throws Exception {
        HugeGraphLoader loader = allocate(HugeGraphLoader.class);
        set(loader, "context", context);
        set(loader, "options", context.options());
        set(loader, "mapping", new LoadMapping(Arrays.asList(inputs)));
        return loader;
    }

    @Test
    public void testUnstartedShutdownPreservesCheckpoint() throws Exception {
        try (ModeServer server = new ModeServer()) {
            LoadOptions options = new LoadOptions();
            options.host = "127.0.0.1";
            options.port = server.server.getAddress().getPort();
            options.file = this.folder.newFile("unstarted.json").getAbsolutePath();
            options.incrementalMode = true;
            File checkpoint = new File(LoadProgress.format(options, "2000-01-01"));
            Files.createDirectories(checkpoint.toPath().getParent());
            String saved = "{\"vertex_progress\":7,\"edge_progress\":0,\"input_progress\":{}}";
            Files.writeString(checkpoint.toPath(), saved);
            HugeGraphLoader loader = new HugeGraphLoader(options, new LoadMapping(Collections.emptyList()));
            Assert.assertEquals(7L, loader.context().oldProgress().vertexLoaded());

            loader.shutdown();

            Assert.assertTrue(loader.context().closed());
            Assert.assertEquals(saved, Files.readString(checkpoint.toPath()));
            Assert.assertFalse(new File(LoadProgress.format(options, loader.context().timestamp())).exists());
        }
    }

    @Test
    public void testEmbeddedShutdownHookDoesNotReplaceExistingCheckpoint() throws Exception {
        try (ModeServer server = new ModeServer()) {
            LoadOptions options = new LoadOptions();
            options.host = "127.0.0.1";
            options.port = server.server.getAddress().getPort();
            options.file = this.folder.newFile("embedded.json").getAbsolutePath();
            options.incrementalMode = true;
            File checkpoint = new File(LoadProgress.format(options, "2000-01-01"));
            Files.createDirectories(checkpoint.toPath().getParent());
            String saved = "{\"vertex_progress\":7,\"edge_progress\":0,\"input_progress\":{}}";
            Files.writeString(checkpoint.toPath(), saved);
            HugeGraphLoader loader = new HugeGraphLoader(options, new LoadMapping(Collections.emptyList()), false);
            Assert.assertEquals(7L, loader.context().oldProgress().vertexLoaded());
            set(loader, "loadStarted", true);
            loader.context().newProgress().plusVertexLoaded(99);

            ((Thread) field(loader, "shutdownHook")).run();

            Assert.assertTrue(loader.context().closed());
            Assert.assertEquals(saved, Files.readString(checkpoint.toPath()));
            Assert.assertFalse(new File(LoadProgress.format(options, loader.context().timestamp())).exists());
        }
    }

    @Test
    public void testDefaultLoaderShutdownStillWritesProgress() throws Exception {
        try (ModeServer server = new ModeServer()) {
            LoadOptions options = new LoadOptions();
            options.host = "127.0.0.1";
            options.port = server.server.getAddress().getPort();
            options.file = this.folder.newFile("standalone.json").getAbsolutePath();
            HugeGraphLoader loader = new HugeGraphLoader(options, new LoadMapping(Collections.emptyList()));
            set(loader, "loadStarted", true);
            loader.context().newProgress().plusVertexLoaded(7);

            loader.shutdown();

            File checkpoint = new File(LoadProgress.format(options, loader.context().timestamp()));
            Assert.assertTrue(loader.context().closed());
            Assert.assertEquals(7L, LoadProgress.read(checkpoint).vertexLoaded());
        }
    }

    @Test
    public void testClosedLoaderUnregistersOwnHook() throws Exception {
        for (boolean failed : new boolean[]{false, true}) {
            try (ModeServer server = new ModeServer()) {
                LoadOptions options = new LoadOptions();
                options.host = "127.0.0.1";
                options.port = server.server.getAddress().getPort();
                options.file = this.folder.newFile("hook-" + failed + ".json").getAbsolutePath();
                HugeGraphLoader loader = new HugeGraphLoader(options, new LoadMapping(Collections.emptyList()));
                Field field = HugeGraphLoader.class.getDeclaredField("shutdownHook");
                field.setAccessible(true);
                Thread hook = (Thread) field.get(loader);
                RuntimeException failure = new IllegalStateException("loading failed");
                try {
                    if (failed) {
                        loader.context().failLoading(failure);
                        try {
                            loader.shutdown();
                            Assert.fail("shutdown must retain the loading failure");
                        } catch (RuntimeException actual) {
                            Assert.assertSame(failure, actual);
                        }
                    } else {
                        loader.shutdown();
                    }
                    Assert.assertTrue(loader.context().closed());
                    Assert.assertFalse(Runtime.getRuntime().removeShutdownHook(hook));
                    loader.shutdown();
                } finally {
                    Runtime.getRuntime().removeShutdownHook(hook);
                }
            }
        }
    }

    @Test
    public void testLoaderResetFailurePreservesIndependentAndLoadingFailures() throws Exception {
        this.assertLoaderShutdownFailure(true);
    }

    @Test
    public void testLoaderSuccessfulResetPreservesLoadingFailureIdentity() throws Exception {
        this.assertLoaderShutdownFailure(false);
    }

    private void assertLoaderShutdownFailure(boolean rejectReset) throws Exception {
        try (ModeServer server = new ModeServer()) {
            server.mode = GraphMode.NONE;
            LoadOptions options = new LoadOptions();
            options.host = "127.0.0.1";
            options.port = server.server.getAddress().getPort();
            options.file = this.folder.newFile("reset-" + rejectReset + ".json").getAbsolutePath();
            HugeGraphLoader loader = new HugeGraphLoader(options, new LoadMapping(Collections.emptyList()));
            Thread hook = (Thread) field(loader, "shutdownHook");
            LoadContext context = loader.context();
            Object manager = field(loader, "manager");
            ExecutorService batch = (ExecutorService) field(manager, "batchService");
            ExecutorService single = (ExecutorService) field(manager, "singleService");
            try {
                // Start both actual native pools; shutdown must drain and terminate them.
                batch.submit(() -> { }).get();
                single.submit(() -> { }).get();
                context.setLoadingMode();
                OkHttpClient transport = (OkHttpClient) field(field(context.client(), "client"), "client");
                RuntimeException original = new IllegalStateException("recorded loading failure");
                context.failLoading(original);
                if (rejectReset) {
                    server.failure = "Independent reset denied";
                }
                RuntimeException actual = null;
                try {
                    loader.shutdown();
                    Assert.fail("Shutdown must report the recorded failure");
                } catch (RuntimeException failure) {
                    actual = failure;
                }
                if (rejectReset) {
                    Assert.assertTrue("Reset failure must remain primary", actual instanceof LoadException);
                    Assert.assertNotSame(original, actual);
                    Assert.assertTrue(actual.getCause() instanceof ServerException);
                    Assert.assertEquals("Independent reset denied", actual.getCause().getMessage());
                    Assert.assertEquals(1, actual.getSuppressed().length);
                    Assert.assertSame(original, actual.getSuppressed()[0]);
                    Assert.assertEquals(GraphMode.LOADING, server.mode);
                    Assert.assertEquals(Boolean.TRUE, field(context, "loadingModeSet"));
                } else {
                    Assert.assertSame(original, actual);
                    Assert.assertEquals(0, actual.getSuppressed().length);
                    Assert.assertEquals(GraphMode.NONE, server.mode);
                    Assert.assertEquals(Boolean.FALSE, field(context, "loadingModeSet"));
                }
                assertAcyclic(actual, Collections.newSetFromMap(new IdentityHashMap<>()));
                Assert.assertTrue(context.closed());
                Assert.assertTrue(batch.isTerminated());
                Assert.assertTrue(single.isTerminated());
                Assert.assertTrue(transport.dispatcher().executorService().isShutdown());
                Assert.assertEquals(0, transport.connectionPool().connectionCount());
                Assert.assertFalse(Runtime.getRuntime().removeShutdownHook(hook));
                Assert.assertEquals(1, server.modeReads);
                Assert.assertEquals(2, server.modeWrites);
                loader.shutdown();
                Assert.assertEquals(1, server.modeReads);
                Assert.assertEquals(2, server.modeWrites);
            } finally {
                server.failure = null;
                try {
                    loader.shutdown();
                } catch (RuntimeException ignored) {
                    // Retain the original assertion while closing the actual native resources.
                }
                Runtime.getRuntime().removeShutdownHook(hook);
            }
        }
    }

    @Test
    public void testLoaderPromotesFatalCloseOverOrdinaryResetFailure() throws Exception {
        this.assertFatalLoaderShutdown(false, false, false);
    }

    @Test
    public void testLoaderPreservesFatalResetAndRecordedLoadingFailure() throws Exception {
        this.assertFatalLoaderShutdown(true, false, false);
    }

    @Test
    public void testLoaderDoesNotSuppressFatalFailureOnItself() throws Exception {
        this.assertFatalLoaderShutdown(true, true, false);
    }

    @Test
    public void testLoaderDoesNotCreateFatalResetCauseCycle() throws Exception {
        this.assertFatalLoaderShutdown(true, false, true);
    }

    private void assertFatalLoaderShutdown(boolean fatalReset, boolean sameError,
                                          boolean causeCycle) throws Exception {
        try (ModeServer server = new ModeServer()) {
            server.mode = GraphMode.NONE;
            LoadOptions options = new LoadOptions();
            options.host = "127.0.0.1";
            options.port = server.server.getAddress().getPort();
            options.file = this.folder.newFile().getAbsolutePath();
            HugeGraphLoader loader = new HugeGraphLoader(options, new LoadMapping(Collections.emptyList()));
            LoadContext context = loader.context();
            Thread hook = (Thread) field(loader, "shutdownHook");
            // Use real SDK clients; only the fatal accessor / post-close boundary is controlled.
            BoundaryClient primary = new BoundaryClient(server);
            HugeClient secondary = server.client();
            context.client().close();
            set(context, "client", primary);
            set(context, "indirectClient", secondary);
            OkHttpClient transport = (OkHttpClient) field(field(primary, "client"), "client");
            OkHttpClient secondaryTransport = (OkHttpClient) field(field(secondary, "client"), "client");
            Object manager = field(loader, "manager");
            ExecutorService batch = (ExecutorService) field(manager, "batchService");
            ExecutorService single = (ExecutorService) field(manager, "singleService");
            Error fatal = new AssertionError("fatal SDK cleanup boundary");
            RuntimeException original = causeCycle ? new IllegalStateException("loading failed", fatal) :
                                                   new IllegalStateException("recorded loading failure");
            try {
                batch.submit(() -> { }).get();
                single.submit(() -> { }).get();
                context.setLoadingMode();
                if (fatalReset) {
                    primary.resetFailure = fatal;
                    if (!sameError) {
                        context.failLoading(original);
                    }
                } else {
                    server.failure = "Independent reset denied";
                }
                primary.closeFailure = !fatalReset || sameError ? fatal : null;
                Throwable actual = null;
                try {
                    loader.shutdown();
                } catch (RuntimeException | Error failure) {
                    actual = failure;
                }
                Assert.assertSame("First fatal Error must escape with its identity", fatal, actual);
                if (causeCycle || sameError) {
                    Assert.assertEquals(0, fatal.getSuppressed().length);
                } else {
                    Assert.assertEquals(1, fatal.getSuppressed().length);
                    if (fatalReset) {
                        Assert.assertSame(original, fatal.getSuppressed()[0]);
                    } else {
                        Assert.assertTrue(fatal.getSuppressed()[0] instanceof LoadException);
                        Assert.assertEquals("Independent reset denied",
                                            fatal.getSuppressed()[0].getCause().getMessage());
                    }
                }
                if (fatalReset && !sameError) {
                    try {
                        context.throwIfFailed();
                        Assert.fail("Context must retain the original recorded failure");
                    } catch (RuntimeException failure) {
                        Assert.assertSame(original, failure);
                    }
                }
                assertAcyclic(fatal, Collections.newSetFromMap(new IdentityHashMap<>()));
                Assert.assertTrue(context.closed());
                Assert.assertTrue(batch.isTerminated());
                Assert.assertTrue(single.isTerminated());
                Assert.assertTrue(transport.dispatcher().executorService().isShutdown());
                Assert.assertEquals(0, transport.connectionPool().connectionCount());
                Assert.assertTrue(secondaryTransport.dispatcher().executorService().isShutdown());
                Assert.assertEquals(0, secondaryTransport.connectionPool().connectionCount());
                Assert.assertEquals(1, primary.closeCalls);
                Assert.assertEquals(GraphMode.LOADING, server.mode);
                Assert.assertEquals(Boolean.TRUE, field(context, "loadingModeSet"));
                Assert.assertFalse(Runtime.getRuntime().removeShutdownHook(hook));
                int reads = server.modeReads;
                int writes = server.modeWrites;
                loader.shutdown();
                Assert.assertEquals(reads, server.modeReads);
                Assert.assertEquals(writes, server.modeWrites);
                Assert.assertEquals(1, primary.closeCalls);
            } finally {
                server.failure = null;
                primary.resetFailure = null;
                primary.closeFailure = null;
                try {
                    loader.shutdown();
                } catch (RuntimeException | Error ignored) {
                    // Preserve assertions while tearing down the real native resources.
                }
                primary.close();
                secondary.close();
                Runtime.getRuntime().removeShutdownHook(hook);
            }
        }
    }

    private static class BoundaryClient extends HugeClient {

        private Error resetFailure;
        private Error closeFailure;
        private int closeCalls;

        private BoundaryClient(ModeServer server) {
            super(HugeClient.builder("http://127.0.0.1:" + server.server.getAddress().getPort(), "hugegraph"));
        }

        @Override
        public GraphsManager graphs() {
            if (this.resetFailure != null) {
                throw this.resetFailure;
            }
            return super.graphs();
        }

        @Override
        public void close() {
            this.closeCalls++;
            super.close();
            if (this.closeFailure != null) {
                throw this.closeFailure;
            }
        }
    }

    private static void assertAcyclic(Throwable failure, Set<Throwable> path) {
        if (failure == null) {
            return;
        }
        Assert.assertTrue("Failure graph must not contain cycles", path.add(failure));
        assertAcyclic(failure.getCause(), path);
        for (Throwable suppressed : failure.getSuppressed()) {
            assertAcyclic(suppressed, path);
        }
        path.remove(failure);
    }

    private static Object field(Object target, String name) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                // SDK transport fields are declared in its existing superclass.
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static class ModeServer implements AutoCloseable {

        private final HttpServer server;
        private volatile GraphMode mode = GraphMode.LOADING;
        private volatile String failure;
        private volatile String readFailure;
        private volatile boolean dropModeResponse;
        private volatile int modeReads;
        private volatile int modeWrites;
        private String ownerCapability;
        private int ownerStatus = 404;
        private boolean dropOwnerReply;
        private boolean dropCapabilityReply;
        private int ownerWriteStatus = 200;
        private String ownerFailure;
        private int ownerWrites;
        private Map<String, Object> lastOwner;

        private void enableOwnership() {
            this.ownerCapability = JsonUtil.toJson(Map.of("protocol", 1, "epoch", 0,
                                                        "instance", UUID.randomUUID().toString()));
            this.ownerStatus = 200;
        }

        private ModeServer() throws IOException {
            this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            this.server.createContext("/", exchange -> {
                String response;
                int status = 200;
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/versions")) {
                    response = "{\"versions\":{\"core\":\"1.8.0\",\"api\":\"0.74\"}}";
                } else if (path.equals("/graphspaces/DEFAULT/graphs/hugegraph/mode/owners")) {
                    status = this.ownerStatus;
                    response = this.ownerCapability == null ?
                               "{\"exception\":\"class jakarta.ws.rs.NotFoundException\"," +
                               "\"message\":\"HTTP 404 Not Found\"}" : this.ownerCapability;
                    if (exchange.getRequestMethod().equals("PUT")) {
                        status = this.ownerWriteStatus;
                        this.ownerWrites++;
                        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                        this.lastOwner = JsonUtil.fromJson(body, Map.class);
                        Map<String, Object> receipt = new HashMap<>(this.lastOwner);
                        boolean acquire = "acquire".equals(receipt.remove("action"));
                        receipt.put("protocol", 1);
                        receipt.put("state", acquire ? "active" : "released");
                        receipt.put("mode", "LOADING");
                        response = this.ownerFailure == null ? JsonUtil.toJson(receipt) :
                                   JsonUtil.toJson(Collections.singletonMap("message", this.ownerFailure));
                    }
                    if (this.dropCapabilityReply || this.dropOwnerReply &&
                        exchange.getRequestMethod().equals("PUT")) {
                        exchange.close();
                        return;
                    }
                } else if (path.equals("/graphspaces/DEFAULT/graphs/hugegraph/mode")) {
                    if (exchange.getRequestMethod().equals("PUT")) {
                        this.modeWrites++;
                        if (this.failure != null) {
                            status = 400;
                            response = JsonUtil.toJson(Collections.singletonMap("message", this.failure));
                        } else {
                            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                            this.mode = GraphMode.valueOf(JsonUtil.fromJson(body, String.class));
                            if (this.dropModeResponse) {
                                exchange.close();
                                return;
                            }
                            response = "{}";
                        }
                    } else {
                        this.modeReads++;
                        if (this.readFailure != null) {
                            status = 400;
                            response = JsonUtil.toJson(Collections.singletonMap("message", this.readFailure));
                        } else {
                            response = JsonUtil.toJson(Collections.singletonMap("mode", this.mode.name()));
                        }
                    }
                } else {
                    status = 404;
                    response = "{}";
                }
                byte[] body = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, body.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(body);
                }
            });
            this.server.start();
        }

        private HugeClient client() {
            return HugeClient.builder("http://127.0.0.1:" + this.server.getAddress().getPort(), "hugegraph")
                             .configTimeout(1).build();
        }

        @Override
        public void close() {
            this.server.stop(0);
        }
    }

    private static void assertClosedWithoutRetrying(LoadContext context, RecordingClient client,
                                                  RecordingClient indirect) {
        Assert.assertTrue(context.closed());
        context.close();
        Assert.assertEquals(1, client.closeCalls);
        Assert.assertEquals(1, indirect.closeCalls);
        Assert.assertEquals(2L, context.newProgress().vertexLoaded());
        Assert.assertEquals(3L, context.newProgress().edgeLoaded());
    }

    private LoadContext context(HugeClient client, HugeClient indirect) throws Exception {
        // Match the existing TaskManagerTest allocation seam: no network clients.
        LoadContext context = allocate(LoadContext.class);
        LoadOptions options = new LoadOptions();
        options.file = this.folder.newFile("mapping-" + UUID.randomUUID() + ".json").getAbsolutePath();
        set(context, "timestamp", "test");
        set(context, "options", options);
        LoadSummary summary = new LoadSummary();
        summary.plusLoaded(ElemType.VERTEX, 2);
        summary.plusLoaded(ElemType.EDGE, 3);
        set(context, "summary", summary);
        set(context, "newProgress", new LoadProgress());
        set(context, "loggers", new HashMap<>());
        set(context, "client", client);
        set(context, "indirectClient", indirect);
        return context;
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafeClass.getMethod("allocateInstance", Class.class)
                                    .invoke(field.get(null), type));
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class RecordingClient extends HugeClient {

        private int closeCalls;
        private String closeFailure;

        private RecordingClient() {
            super((HugeClientBuilder) null);
        }

        @Override
        public void close() {
            this.closeCalls++;
            if (this.closeFailure != null) {
                throw new IllegalStateException(this.closeFailure);
            }
        }
    }
}
