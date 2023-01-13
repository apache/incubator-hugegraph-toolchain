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

package org.apache.hugegraph.options;

import static org.apache.hugegraph.config.OptionChecker.allowValues;
import static org.apache.hugegraph.config.OptionChecker.disallowEmpty;
import static org.apache.hugegraph.config.OptionChecker.positiveInt;
import static org.apache.hugegraph.config.OptionChecker.rangeInt;

import org.springframework.util.CollectionUtils;

import org.apache.hugegraph.config.ConfigListOption;
import org.apache.hugegraph.config.ConfigOption;
import org.apache.hugegraph.config.OptionHolder;
import org.apache.hugegraph.util.Bytes;
import org.apache.hugegraph.util.HubbleUtil;

public class HubbleOptions extends OptionHolder {

    private HubbleOptions() {
        super();
    }

    private static volatile HubbleOptions instance;

    public static synchronized HubbleOptions instance() {
        if (instance == null) {
            instance = new HubbleOptions();
            instance.registerOptions();
        }
        return instance;
    }

    public static final ConfigOption<String> SERVER_ID =
            new ConfigOption<>(
                    "server.id",
                    "The id of hugegraph-hubble server.",
                    disallowEmpty(),
                    "hubble-1"
            );

    public static final ConfigOption<String> SERVER_HOST =
            new ConfigOption<>(
                    "hubble.host",
                    "The host of hugegraph-hubble server.",
                    disallowEmpty(),
                    "localhost"
            );

    public static final ConfigOption<Integer> SERVER_PORT =
            new ConfigOption<>(
                    "hubble.port",
                    "The port of hugegraph-hubble server.",
                    rangeInt(1, 65535),
                    8088
            );

    public static final ConfigListOption<String> CONNECTION_IP_WHITE_LIST =
            new ConfigListOption<>(
                    "graph_connection.ip_white_list",
                    "The ip white list available for connecting to " +
                    "HugeGraphServer, * means no ip limited.",
                    input -> {
                        if (CollectionUtils.isEmpty(input)) {
                            return false;
                        }
                        if (input.contains("*") && input.size() > 1) {
                            return false;
                        }
                        for (String ip : input) {
                            if (!HubbleUtil.HOST_PATTERN.matcher(ip)
                                                        .matches()) {
                                return false;
                            }
                        }
                        return true;
                    },
                    "*"
            );

    public static final ConfigListOption<Integer> CONNECTION_PORT_WHITE_LIST =
            new ConfigListOption<>(
                    "graph_connection.port_white_list",
                    "The port white list available for connecting to " +
                    "HugeGraphServer, -1 means no port limited.",
                    input -> {
                        if (CollectionUtils.isEmpty(input)) {
                            return false;
                        }
                        return !input.contains(-1) || input.size() <= 1;
                    },
                    -1
            );

    public static final ConfigOption<Integer> CLIENT_REQUEST_TIMEOUT =
            new ConfigOption<>(
                    "client.request_timeout",
                    "The request timeout in seconds for HugeClient.",
                    positiveInt(),
                    60
            );

    public static final ConfigOption<Integer> GREMLIN_SUFFIX_LIMIT =
            new ConfigOption<>(
                    "gremlin.suffix_limit",
                    "The limit suffix to be added to gremlin statement.",
                    rangeInt(1, 800000),
                    250
            );

    public static final ConfigOption<Integer> GREMLIN_VERTEX_DEGREE_LIMIT =
            new ConfigOption<>(
                    "gremlin.vertex_degree_limit",
                    "The max edges count for per vertex.",
                    rangeInt(1, 500),
                    100
            );

    public static final ConfigOption<Integer> GREMLIN_EDGES_TOTAL_LIMIT =
            new ConfigOption<>(
                    "gremlin.edges_total_limit",
                    "The edges total limit.",
                    rangeInt(1, 1000),
                    500
            );

    public static final ConfigOption<Integer> GREMLIN_BATCH_QUERY_IDS =
            new ConfigOption<>(
                    "gremlin.batch_query_ids",
                    "The ids count for every batch.",
                    rangeInt(1, 250),
                    100
            );

    public static final ConfigOption<Integer> EXECUTE_HISTORY_SHOW_LIMIT =
            new ConfigOption<>(
                    "execute-history.show_limit",
                    "The show limit of execute histories.",
                    rangeInt(0, 10000),
                    500
            );

    public static final ConfigOption<String> UPLOAD_FILE_LOCATION =
            new ConfigOption<>(
                    "upload_file.location",
                    "The location of uploaded files.",
                    disallowEmpty(),
                    "upload-files"
            );

    public static final ConfigListOption<String> UPLOAD_FILE_FORMAT_LIST =
            new ConfigListOption<>(
                    "upload_file.format_list",
                    "The format white list available for uploading file.",
                    null,
                    "csv"
            );

    public static final ConfigOption<Long> UPLOAD_SINGLE_FILE_SIZE_LIMIT =
            new ConfigOption<>(
                    "upload_file.single_file_size_limit",
                    "The single file size(MB) limit.",
                    positiveInt(),
                    1 * Bytes.GB
            );

    public static final ConfigOption<Long> UPLOAD_TOTAL_FILE_SIZE_LIMIT =
            new ConfigOption<>(
                    "upload_file.total_file_size_limit",
                    "The total file size(MB) limit.",
                    positiveInt(),
                    10 * Bytes.GB
            );

    public static final ConfigOption<Long> UPLOAD_FILE_MAX_TIME_CONSUMING =
            new ConfigOption<>(
                    "upload_file.max_uploading_time",
                    "The maximum allowable uploading time(second) for file " +
                    "uploads, the uploaded file parts will be cleared if " +
                    "exceed this time",
                    positiveInt(),
                    12L * 60 * 60
            );

    public static final ConfigOption<String> SERVER_PROTOCOL =
            new ConfigOption<>(
                    "server.protocol",
                    "The protocol of HugeGraphServer, allowed values are: " +
                    "http or https",
                    allowValues("http", "https"),
                    "http"
            );

    public static final ConfigOption<String> CLIENT_TRUSTSTORE_FILE =
            new ConfigOption<>(
                    "ssl.client_truststore_file",
                    "The path of the client truststore file " +
                    "when https protocol is enabled",
                    null,
                    "conf/hugegraph.truststore"
            );

    public static final ConfigOption<String> CLIENT_TRUSTSTORE_PASSWORD =
            new ConfigOption<>(
                    "ssl.client_truststore_password",
                    "The password of the client truststore " +
                    "when https protocol is enabled",
                    null,
                    "hugegraph"
            );
}
