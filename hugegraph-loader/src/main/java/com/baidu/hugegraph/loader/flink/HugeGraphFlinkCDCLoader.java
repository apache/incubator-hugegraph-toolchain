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

package com.baidu.hugegraph.loader.flink;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.builder.ElementBuilder;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.mapping.LoadMapping;
import com.baidu.hugegraph.loader.source.jdbc.JDBCSource;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.util.Log;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;

public class HugeGraphFlinkCDCLoader {
    public static final Logger LOG = Log.logger(HugeGraphFlinkCDCLoader.class);

    private final LoadOptions loadOptions;
    private final Map<ElementBuilder, List<GraphElement>> builders;
    private final String[] options;

    public HugeGraphFlinkCDCLoader(String[] args) {
        this.options = args;
        this.loadOptions = LoadOptions.parseOptions(args);
        this.builders = new HashMap<>();
    }

    public static void main(String[] args) {
        String[] args1 = new String[]{
                "-g", "talent_graph"
                , "-h", "10.22.21.33"
                , "-p", "8093"
                , "--username", "admin"
                , "--token", "dm@cvte"
                , "-f", "/Users/zsm/Desktop/code-github/github/hugegraph-all/" +
                        "hugegraph-toolchain/hugegraph-loader/config/flink-test.json"
                , "-s", "/Users/zsm/Desktop/code-github/github/hugegraph-all/" +
                        "hugegraph-toolchain/hugegraph-loader/config/schema-talent.groovy"
                , "--realtime-flush-interval", "10000"
        };
        HugeGraphFlinkCDCLoader loader;
        try {
            loader = new HugeGraphFlinkCDCLoader(args1);
        } catch (Throwable e) {
            Printer.printError("Failed to start loading", e);
            return;
        }
        loader.load();
    }

    public void load() {
        //StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
        LoadMapping mapping = LoadMapping.of(this.loadOptions.file);
        List<InputStruct> structs = mapping.structs();
        //StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        for (InputStruct struct : structs) {

            JDBCSource input = (JDBCSource) struct.input();
            String charset = input.charset();
            String[] header = input.header();

            String[] url = input.url().split("//")[1].split(":");
            MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                    .hostname(url[0])
                    .port(Integer.parseInt(url[1]))
                    .databaseList(input.database())
                    .tableList(input.database() + "." + input.table())
                    .username(input.username())
                    .password(input.password())
                    // converts SourceRecord to JSON String
                    .deserializer(new HugeGraphDeserialization())
                    .build();


            DataStreamSource<String> source =
                    env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source");

            LoadContext loadContext = new LoadContext(loadOptions);

            DataStreamSink<String> rowDataStreamSink = source.addSink(
                    new HugeGraphSinkFunction<>(
                            new HugeGraphOutputFormat<>(struct, options)));


            if (true) {
                rowDataStreamSink.setParallelism(1);
            }

        }

        env.enableCheckpointing(3000);

        try {
            env.execute("Print MySQL Snapshot + Binlog");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
