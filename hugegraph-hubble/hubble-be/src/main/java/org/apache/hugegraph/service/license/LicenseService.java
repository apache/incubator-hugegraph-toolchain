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

package org.apache.hugegraph.service.license;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.handler.MessageSourceHandler;
import org.apache.hugegraph.service.GraphConnectionService;
import org.apache.hugegraph.service.HugeClientPoolService;
import org.apache.hugegraph.util.Ex;

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

        private final boolean enabled;
        private final String graphsMessage;
        private final List<String> dataSizeMessages;

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
        return new VerifyResult(true);
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

        conn.setEnabled(true);
        conn.setDisableReason("");
        this.connService.update(conn);
    }

    private String getMessage(String msgKey, Object... args) {
        return this.messageHandler.getMessage(msgKey, args);
    }

    /**
     * Keep 2 method for future use now
     */
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
            default:
                break;
        }
        return byteCount / FileUtils.ONE_MB;
    }
}
