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

package com.baidu.hugegraph.loader.task;

import static com.baidu.hugegraph.loader.constant.Constants.BATCH_PRINT_FREQ;

import java.util.List;

import org.slf4j.Logger;

import com.baidu.hugegraph.exception.ServerException;
import com.baidu.hugegraph.loader.builder.Record;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.rest.ClientException;
import com.baidu.hugegraph.util.Log;

public class BatchInsertTask extends InsertTask {

    private static final Logger LOG = Log.logger(TaskManager.class);

    public BatchInsertTask(InputStruct struct, ElementMapping mapping,
                           List<Record> batch) {
        super(struct, mapping, batch);
    }

    @Override
    public void run() {
        boolean checkVertex = this.options().checkVertex;
        int retryCount = 0;
        do {
            try {
                if (this.mapping.updateStrategies().isEmpty()) {
                    this.addBatch(this.batch, checkVertex);
                } else {
                    this.updateBatch(this.batch, checkVertex);
                }
                break;
            } catch (ClientException e) {
                LOG.debug("client exception: {}", e.getMessage());
                retryCount = this.waitThenRetry(retryCount, e);
            } catch (ServerException e) {
                LOG.debug("server exception: {}", e.getMessage());
                if (UNACCEPTABLE_EXCEPTIONS.contains(e.exception())) {
                    throw e;
                }
                retryCount = this.waitThenRetry(retryCount, e);
            }
        } while (retryCount > 0 && retryCount <= this.options().retryTimes);

        int count = this.batch.size();
        // This metrics just for current element mapping
        this.plusLoadSuccess(count);
        Printer.printProgress(this.type(), BATCH_PRINT_FREQ, count);
    }

    private int waitThenRetry(int retryCount, RuntimeException e) {
        LoadOptions options = this.options();
        if (options.retryTimes <= 0) {
            return retryCount;
        }

        if (++retryCount > options.retryTimes) {
            LOG.error("Batch insert has been retried more than {} times",
                      options.retryTimes);
            throw e;
        }

        long interval = (1L << retryCount) * options.retryInterval;
        LOG.debug("Batch insert will sleep {} seconds then do the {}th retry",
                  interval, retryCount);
        try {
            Thread.sleep(interval * 1000L);
        } catch (InterruptedException ignored) {
            // That's fine, just continue.
        }
        return retryCount;
    }
}
