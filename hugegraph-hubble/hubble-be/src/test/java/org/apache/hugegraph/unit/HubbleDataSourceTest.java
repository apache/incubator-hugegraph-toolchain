/*
 *
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

package org.apache.hugegraph.unit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.hugegraph.config.HubbleDataSource;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class HubbleDataSourceTest {

    @Test
    public void testPreInterruptedCloseCompletesPoolCleanup() throws Exception {
        this.closePool(true, false);
    }

    @Test
    public void testInterruptDuringConcurrentCloseCompletesPoolCleanup() throws Exception {
        this.closePool(false, true);
    }

    @Test
    public void testUninitializedDatasourceClosesRepeatedly() {
        HubbleDataSource datasource = new HubbleDataSource();
        datasource.close();
        datasource.close();
        assertTrue(datasource.isClosed());
    }

    private void closePool(boolean preInterrupted, boolean concurrentClose) throws Exception {
        HubbleDataSource datasource = new HubbleDataSource();
        datasource.setJdbcUrl("jdbc:h2:mem:metadata-close-" + preInterrupted);
        datasource.setMaximumPoolSize(1);
        datasource.setMinimumIdle(0);
        Connection physical;
        try (Connection connection = datasource.getConnection()) {
            physical = connection.unwrap(Connection.class);
        }
        Object pool = datasource.getHikariPoolMXBean();
        ThreadPoolExecutor addConnections =
                (ThreadPoolExecutor) ReflectionTestUtils.getField(pool, "addConnectionExecutor");
        List<ExecutorService> ownedExecutors = new ArrayList<>();
        ownedExecutors.add(addConnections);
        ownedExecutors.add((ExecutorService) ReflectionTestUtils.getField(pool, "closeConnectionExecutor"));
        ownedExecutors.add((ExecutorService) ReflectionTestUtils.getField(pool, "houseKeepingExecutorService"));
        Object networkTimeout = ReflectionTestUtils.getField(pool, "netTimeoutExecutor");
        if (networkTimeout instanceof ExecutorService) {
            ownedExecutors.add((ExecutorService) networkTimeout);
        }

        CountDownLatch addTaskEntered = new CountDownLatch(1);
        CountDownLatch releaseAddTask = new CountDownLatch(1);
        CountDownLatch nativeAwaitEntered = new CountDownLatch(1);
        // Keep real Hikari cleanup inside its first interruptible wait. Physical
        // connection closure alone can race ahead and hide the incomplete cleanup.
        Future<?> blockedAdd = addConnections.submit(() -> {
            addTaskEntered.countDown();
            assertTrue("The controlled connection task must be released",
                       releaseAddTask.await(15, TimeUnit.SECONDS));
            return null;
        });
        ThreadPoolExecutor observedAdds = Mockito.mock(ThreadPoolExecutor.class,
                AdditionalAnswers.delegatesTo(addConnections));
        Mockito.doAnswer(invocation -> {
            nativeAwaitEntered.countDown();
            return addConnections.awaitTermination(invocation.getArgument(0), invocation.getArgument(1));
        }).when(observedAdds).awaitTermination(Mockito.anyLong(), Mockito.any(TimeUnit.class));
        ReflectionTestUtils.setField(pool, "addConnectionExecutor", observedAdds);

        CountDownLatch firstReturned = new CountDownLatch(1);
        FutureTask<Boolean> firstClose = new FutureTask<>(() -> {
            if (preInterrupted) {
                Thread.currentThread().interrupt();
            }
            datasource.close();
            firstReturned.countDown();
            return Thread.currentThread().isInterrupted();
        });
        Thread firstCaller = new Thread(firstClose, "metadata-first-close-test");
        CountDownLatch secondEntered = new CountDownLatch(1);
        CountDownLatch secondReturned = new CountDownLatch(1);
        FutureTask<Boolean> secondClose = new FutureTask<>(() -> {
            secondEntered.countDown();
            datasource.close();
            secondReturned.countDown();
            return Thread.currentThread().isInterrupted();
        });
        Thread secondCaller = new Thread(secondClose, "metadata-second-close-test");
        try {
            assertTrue(addTaskEntered.await(10, TimeUnit.SECONDS));
            firstCaller.start();
            assertTrue("Shutdown must reach the real Hikari executor wait",
                       nativeAwaitEntered.await(10, TimeUnit.SECONDS));
            assertFalse("Close must wait for pool cleanup despite an existing interrupt",
                        firstReturned.await(100, TimeUnit.MILLISECONDS));
            if (concurrentClose) {
                secondCaller.start();
                assertTrue(secondEntered.await(10, TimeUnit.SECONDS));
                assertFalse("Concurrent close must await the ongoing cleanup",
                            secondReturned.await(100, TimeUnit.MILLISECONDS));
                firstCaller.interrupt();
                secondCaller.interrupt();
                assertFalse("A new interrupt must not abandon pool cleanup",
                            firstReturned.await(100, TimeUnit.MILLISECONDS));
            }
            releaseAddTask.countDown();
            blockedAdd.get(10, TimeUnit.SECONDS);
            assertTrue("Close must restore the caller's interrupt",
                       firstClose.get(10, TimeUnit.SECONDS));
            if (concurrentClose) {
                assertTrue("Concurrent close must restore its caller's interrupt",
                           secondClose.get(10, TimeUnit.SECONDS));
            }
            assertTrue("The real H2 connection must be physically closed", physical.isClosed());
            for (ExecutorService executor : ownedExecutors) {
                assertTrue("Every Hikari-owned executor must be shut down", executor.isShutdown());
                assertTrue("Every Hikari-owned executor must terminate",
                           executor.awaitTermination(10, TimeUnit.SECONDS));
            }
            datasource.close();
        } finally {
            releaseAddTask.countDown();
            // Also clean up the real resources when running against the unfixed
            // close implementation, whose closed flag prevents a second attempt.
            for (ExecutorService executor : ownedExecutors) {
                executor.shutdownNow();
            }
            firstCaller.join(TimeUnit.SECONDS.toMillis(10));
            secondCaller.join(TimeUnit.SECONDS.toMillis(10));
            datasource.close();
            physical.close();
            for (ExecutorService executor : ownedExecutors) {
                assertTrue("Test-owned Hikari resource cleanup must terminate",
                           executor.awaitTermination(10, TimeUnit.SECONDS));
            }
        }
    }
}
