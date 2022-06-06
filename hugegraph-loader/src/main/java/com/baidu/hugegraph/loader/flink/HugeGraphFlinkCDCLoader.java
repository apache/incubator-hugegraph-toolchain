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

import static java.util.regex.Pattern.compile;

import java.util.List;
import java.util.regex.Matcher;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.mapping.LoadMapping;
import com.baidu.hugegraph.loader.source.jdbc.JDBCSource;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;

public class HugeGraphFlinkCDCLoader {
    public static final Logger LOG = Log.logger(HugeGraphFlinkCDCLoader.class);

    private final LoadOptions loadOptions;
    private final String[] options;

    public HugeGraphFlinkCDCLoader(String[] args) {
        this.options = args;
        this.loadOptions = LoadOptions.parseOptions(args);
    }

    public static void main(String[] args) {
        HugeGraphFlinkCDCLoader loader;
        try {
            loader = new HugeGraphFlinkCDCLoader(args);
        } catch (Throwable e) {
            Printer.printError("Failed to start loading", e);
            return;
        }
        loader.load();
    }

    public void load() {
        LoadMapping mapping = LoadMapping.of(this.loadOptions.file);
        List<InputStruct> structs = mapping.structs();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        for (InputStruct struct : structs) {
            JDBCSource input = (JDBCSource) struct.input();
            String host = "";
            int port = -1;
            Matcher m = compile(Constants.HOST_PORT_REGEX).matcher(input.url());
            while (m.find()) {
                host = m.group(1);
                port = Integer.parseInt(m.group(2));
            }
            E.checkArgument(!"".equals(host) && port != -1,
                            String.format("Failed to parse Url(%s) to get hostName and port",
                                          input.url()));

            MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                    .hostname(host)
                    .port(port)
                    .databaseList(input.database())
                    .tableList(input.database() + "." + input.table())
                    .username(input.username())
                    .password(input.password())
                    .deserializer(new HugeGraphDeserialization())
                    .build();

            DataStreamSource<String> source =
                    env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source");

            HugeGraphOutputFormat<Object> format = new HugeGraphOutputFormat<>(struct, options);
            source.addSink(new HugeGraphSinkFunction<>(format)).setParallelism(1);
        }
        env.enableCheckpointing(3000);
        try {
            env.execute("flink-cdc-hugegraph");
        } catch (Exception e) {
            Printer.printError("Failed to execute flink.", e);
        }
    }
}
