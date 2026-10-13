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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.hugegraph.config.AsyncConfig;
import org.junit.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.interceptor.AsyncExecutionAspectSupport;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.function.SingletonSupplier;

public class AsyncConfigLifecycleTest {

    @Test
    public void testContextCloseTerminatesAsyncWorkers() throws Throwable {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(AsyncConfig.class, AsyncProbe.class);
        context.refresh();
        CountDownLatch started = new CountDownLatch(8);
        CountDownLatch release = new CountDownLatch(1);
        Set<Thread> workers = ConcurrentHashMap.newKeySet();
        ThreadPoolTaskExecutor executor = null;
        Throwable primaryFailure = null;
        try {
            AsyncProbe probe = context.getBean(AsyncProbe.class);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            futures.add(probe.execute(started, release, workers));
            executor = configuredExecutor(probe);
            for (int i = 1; i < 8; i++) {
                futures.add(probe.execute(started, release, workers));
            }
            assertTrue("All eight core workers must start", started.await(10, TimeUnit.SECONDS));
            assertEquals(8, workers.size());
            assertEquals(8, executor.getPoolSize());
            for (Thread worker : workers) {
                assertFalse(worker.isDaemon());
            }
            release.countDown();
            for (CompletableFuture<Void> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
            assertFalse(executor.getThreadPoolExecutor().isShutdown());
            AsyncConfig config = context.getBean(AsyncConfig.class);
            boolean managed = context.containsBean("getAsyncExecutor");
            if (managed) {
                assertSame(executor, context.getBean("getAsyncExecutor"));
                assertSame(executor, config.getAsyncExecutor());
                assertSame(executor, config.getAsyncExecutor());
            }
            context.close();

            assertTrue("Context close must shut down the actual async executor",
                       executor.getThreadPoolExecutor().isShutdown());
            assertTrue("The drained executor must terminate",
                       executor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS));
            for (Thread worker : workers) {
                worker.join(TimeUnit.SECONDS.toMillis(10));
                assertFalse("The owned async worker must exit: " + worker.getName(), worker.isAlive());
            }
            assertTrue("The async executor must be a managed Spring bean", managed);
        } catch (Throwable failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            release.countDown();
            Throwable cleanupFailure = null;
            try {
                context.close();
            } catch (Throwable failure) {
                cleanupFailure = failure;
            } finally {
                if (executor != null) {
                    try {
                        // The unfixed configuration is unmanaged; clean up this test's own pool on RED.
                        executor.shutdown();
                        assertTrue("Test-owned executor cleanup must terminate",
                                   executor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS));
                    } catch (Throwable failure) {
                        if (cleanupFailure == null) {
                            cleanupFailure = failure;
                        } else if (cleanupFailure != failure) {
                            cleanupFailure.addSuppressed(failure);
                        }
                    }
                }
            }
            if (cleanupFailure != null) {
                if (primaryFailure == null) {
                    throw cleanupFailure;
                }
                if (primaryFailure != cleanupFailure) {
                    primaryFailure.addSuppressed(cleanupFailure);
                }
            }
        }
    }

    private static ThreadPoolTaskExecutor configuredExecutor(AsyncProbe probe) {
        for (Advisor advisor : ((Advised) probe).getAdvisors()) {
            if (advisor.getAdvice() instanceof AsyncExecutionAspectSupport) {
                // Observe the pool selected by the real @Async dispatcher, including on RED
                // where AsyncConfig does not publish a managed executor bean.
                Object supplier = ReflectionTestUtils.getField(advisor.getAdvice(), "defaultExecutor");
                assertNotNull(supplier);
                return (ThreadPoolTaskExecutor) ((SingletonSupplier<?>) supplier).get();
            }
        }
        throw new AssertionError("The probe must use Spring's async dispatcher");
    }

    public static class AsyncProbe {

        @Async
        public CompletableFuture<Void> execute(CountDownLatch started,
                                                CountDownLatch release,
                                                Set<Thread> workers) throws InterruptedException {
            workers.add(Thread.currentThread());
            started.countDown();
            assertTrue("The controlled async task must be released", release.await(10, TimeUnit.SECONDS));
            return CompletableFuture.completedFuture(null);
        }
    }
}
