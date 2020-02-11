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

package com.baidu.hugegraph.options;

import static com.baidu.hugegraph.config.OptionChecker.disallowEmpty;
import static com.baidu.hugegraph.config.OptionChecker.rangeInt;

import com.baidu.hugegraph.config.ConfigOption;
import com.baidu.hugegraph.config.OptionHolder;

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
                    "server.host",
                    "The host of hugegraph-hubble server.",
                    disallowEmpty(),
                    "localhost"
            );

    public static final ConfigOption<Integer> SERVER_PORT =
            new ConfigOption<>(
                    "server.port",
                    "The port of hugegraph-hubble server.",
                    rangeInt(1, 65535),
                    8088
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
                    rangeInt(1, 500),
                    100
            );

    public static final ConfigOption<Integer> EXECUTE_HISTORY_SHOW_LIMIT =
            new ConfigOption<>(
                    "execute-history.show_limit",
                    "The show limit of execute histories.",
                    rangeInt(0, 10000),
                    500
            );

    public static final ConfigOption<Integer> INDEXLABEL_REBUILD_TIMEOUT =
            new ConfigOption<>(
                    "indexlabel.rebuild.timeout",
                    "The timeout in seconds for waiting to create index label.",
                    rangeInt(-1, Integer.MAX_VALUE),
                    30
            );

    public static final ConfigOption<Integer> INDEXLABEL_REMOVE_TIMEOUT =
            new ConfigOption<>(
                    "indexlabel.remove.timeout",
                    "The timeout in seconds for waiting to remove index label.",
                    rangeInt(-1, Integer.MAX_VALUE),
                    30
            );
}
