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

package org.apache.hugegraph.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean
    @DependsOn("dataSource")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor() {
            @Override
            public void destroy() {
                try {
                    super.destroy();
                } finally {
                    awaitTermination(this.getThreadPoolExecutor());
                }
            }
        };
        taskExecutor.setCorePoolSize(8);
        taskExecutor.setMaxPoolSize(16);
        taskExecutor.setQueueCapacity(1024);
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        return taskExecutor;
    }

    @Bean
    @DependsOn("dataSource")
    public ThreadPoolTaskScheduler maintenanceScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler() {
            @Override
            public void destroy() {
                try {
                    super.destroy();
                } finally {
                    awaitTermination(this.getScheduledExecutor());
                }
            }
        };
        // One slot for each periodic method keeps slow cleanup from blocking
        // progress updates. Native periodic tasks cannot overlap themselves.
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("hubble-maintenance-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }

    private static void awaitTermination(ExecutorService executor) {
        // A native shutdown may throw after initiating shutdown.
        // Drain in that case too; never await a pool still accepting.
        if (!executor.isShutdown()) {
            return;
        }
        boolean interrupted = false;
        try {
            while (!executor.isTerminated()) {
                try {
                    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
