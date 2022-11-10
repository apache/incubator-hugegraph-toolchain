/*
 * Copyright 2017 HugeGraph Authors
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

package org.apache.hugegraph.handler;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import org.apache.hugegraph.entity.load.LoadTask;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class LoadTaskExecutor {

    @Async
    public void execute(LoadTask task, Runnable callback) {
        log.info("Executing task: {}", task.getId());
        task.run();
        log.info("Executed task: {}, update status to db", task.getId());
        callback.run();
    }
}
