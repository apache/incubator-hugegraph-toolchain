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

package com.baidu.hugegraph.service.license;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.baidu.hugegraph.advisor.MessageSourceHandler;
import com.baidu.hugegraph.common.Constant;
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.entity.GraphConnection;
import com.baidu.hugegraph.exception.ExternalException;
import com.baidu.hugegraph.license.LicenseVerifier;
import com.baidu.hugegraph.service.GraphConnectionService;
import com.baidu.hugegraph.service.HugeClientPoolService;
import com.baidu.hugegraph.util.Ex;

import lombok.AllArgsConstructor;
import lombok.Data;

@Service
public class LicenseService {

    private static final String METRICS_DATA_SIZE = "data_size";

    @Autowired
    private GraphConnectionService connService;
    @Autowired
    private HugeClientPoolService poolService;
    @Autowired
    private MessageSourceHandler messageHandler;

    @Data
    @AllArgsConstructor
    public class VerifyResult {

        private boolean enabled;
        private String graphsMessage;
        private List<String> dataSizeMessages;

        public VerifyResult(boolean enabled) {
            this(enabled, null);
        }

        public VerifyResult(boolean enabled, String graphsMessage) {
            this.enabled = enabled;
            this.graphsMessage = graphsMessage;
            this.dataSizeMessages = new ArrayList<>();
        }

        public void add(String disableReason) {
            this.dataSizeMessages.add(disableReason);
        }

        public String getMessage() {
            if (this.enabled) {
                return null;
            }

            String comma = LicenseService.this.getMessage("common.joiner.comma");
            String semicolon = LicenseService.this.getMessage(
                                                   "common.joiner.semicolon");

            StringBuilder sb = new StringBuilder();
            sb.append(LicenseService.this.getMessage(
                      "license.verify.graph-connection.failed.preifx"));
            sb.append(comma);
            if (!StringUtils.isEmpty(this.graphsMessage)) {
                sb.append(this.graphsMessage);
                sb.append(semicolon);
            }
            if (!this.dataSizeMessages.isEmpty()) {
                for (String dataSizeMsg : this.dataSizeMessages) {
                    if (!StringUtils.isEmpty(dataSizeMsg)) {
                        sb.append(dataSizeMsg);
                        sb.append(comma);
                    }
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(comma);
            sb.append(LicenseService.this.getMessage(
                      "license.verify.graph-connection.failed.suffix"));
            return sb.toString();
        }
    }

    public VerifyResult verifyGraphs(int actualGraphs) {
        int allowedGraphs = LicenseVerifier.instance().allowedGraphs();
        if (allowedGraphs != Constant.NO_LIMIT &&
            actualGraphs > allowedGraphs) {
            String msg = this.getMessage("license.verify.graphs.exceed",
                                         actualGraphs, allowedGraphs);
            return new VerifyResult(false, msg);
        } else {
            return new VerifyResult(true);
        }
    }

    public VerifyResult verifyDataSize(HugeClient client, String name,
                                       String graph) {
        long allowedDataSize = LicenseVerifier.instance().allowedDataSize();
        long actualDataSize = getActualDataSize(client, graph);
        if (allowedDataSize != Constant.NO_LIMIT &&
            actualDataSize > allowedDataSize) {
            String msg = this.getMessage("license.verify.datasize.exceed",
                                         name, actualDataSize, allowedDataSize);
            return new VerifyResult(false, msg);
        } else {
            return new VerifyResult(true);
        }
    }

    public void checkGraphStatus(int connId) {
        GraphConnection connection = this.connService.get(connId);
        Ex.check(connection != null, "graph-connection.not-exist.id", connId);

        int actualGraphs = this.connService.count();
        VerifyResult verifyResult = this.verifyGraphs(actualGraphs);
        String msg = null;
        if (!verifyResult.isEnabled() && !connection.getEnabled()) {
            msg = String.format("%s, %s", verifyResult.getMessage(),
                                connection.getDisableReason());
        } else if (!verifyResult.isEnabled() && connection.getEnabled()) {
            msg = verifyResult.getMessage();
        } else if (verifyResult.isEnabled() && !connection.getEnabled()) {
            msg = connection.getDisableReason();
        }
        if (msg != null) {
            throw new ExternalException(Constant.STATUS_UNAUTHORIZED, msg);
        }
    }

    @Async
    @Scheduled(fixedRate = 3 * 60 * 1000)
    public void updateAllGraphStatus() {
        List<GraphConnection> connections = this.connService.listAll();
        for (GraphConnection conn : connections) {
            this.updateGraphStatus(conn);
        }
    }

    private void updateGraphStatus(GraphConnection conn) {
        HugeClient client;
        try {
            client = this.poolService.getOrCreate(conn.getId());
        } catch (Exception e) {
            String msg = this.getMessage("graph-connection.client.unavailable",
                                         conn.getName());
            conn.setEnabled(false);
            conn.setDisableReason(msg);
            this.connService.update(conn);
            return;
        }

        long allowedDataSize = LicenseVerifier.instance().allowedDataSize();
        long actualDataSize = getActualDataSize(client, conn.getGraph());
        if (allowedDataSize != Constant.NO_LIMIT &&
            actualDataSize > allowedDataSize) {
            String msg = this.getMessage("license.verify.datasize.exceed",
                                         conn.getName(), actualDataSize,
                                         allowedDataSize);
            conn.setEnabled(false);
            conn.setDisableReason(msg);
            this.connService.update(conn);
            return;
        }

        conn.setEnabled(true);
        conn.setDisableReason("");
        this.connService.update(conn);
    }

    private String getMessage(String msgKey, Object... args) {
        return this.messageHandler.getMessage(msgKey, args);
    }

    private static long getActualDataSize(HugeClient client, String graph) {
        Map<String, Object> metrics = client.metrics().backend(graph);
        Object dataSize = metrics.get(METRICS_DATA_SIZE);
        if (dataSize == null) {
            return 0L;
        }
        Ex.check(dataSize instanceof String,
                 "The backend metrics data_size must be String type, " +
                 "but got '%s'(%s)", dataSize, dataSize.getClass());
        // Unit is MB
        return displaySizeToMB((String) dataSize);
    }

    private static long displaySizeToMB(String displaySize) {
        String[] parts = displaySize.split(" ");
        Ex.check(parts.length == 2,
                 "The displaySize must be formatted as two parts");
        long numberPart = Long.parseLong(parts[0]);
        long byteCount = 0L;
        switch (parts[1]) {
            case "bytes":
                byteCount = numberPart;
                break;
            case "KB":
                byteCount = numberPart * FileUtils.ONE_KB;
                break;
            case "MB":
                byteCount = numberPart * FileUtils.ONE_MB;
                break;
            case "GB":
                byteCount = numberPart * FileUtils.ONE_GB;
                break;
            case "TB":
                byteCount = numberPart * FileUtils.ONE_TB;
                break;
            case "PB":
                byteCount = numberPart * FileUtils.ONE_PB;
                break;
            case "EB":
                byteCount = numberPart * FileUtils.ONE_EB;
                break;
        }
        return byteCount / FileUtils.ONE_MB;
    }
}
