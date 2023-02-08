/*
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

package org.apache.hugegraph.loader.flink;

import java.net.URISyntaxException;
import java.util.List;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.http.client.utils.URIBuilder;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.LoadMapping;
import org.apache.hugegraph.loader.source.jdbc.JDBCSource;
import org.apache.hugegraph.loader.util.Printer;
import org.slf4j.Logger;

import org.apache.hugegraph.util.Log;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;

public class HugeGraphFlinkCDCLoader {

    public static final Logger LOG = Log.logger(HugeGraphFlinkCDCLoader.class);
    private static final String JDBC_PREFIX = "jdbc:";
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
            MySqlSource<String> mysqlSource = buildMysqlSource(struct);
            DataStreamSource<String> source =
                    env.fromSource(mysqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source");

            HugeGraphOutputFormat<Object> format = new HugeGraphOutputFormat<>(struct, options);
            source.addSink(new HugeGraphSinkFunction<>(format))
                  .setParallelism(this.loadOptions.sinkParallelism);
        }
        env.enableCheckpointing(3000);
        try {
            env.execute("flink-cdc-hugegraph");
        } catch (Exception e) {
            throw new LoadException("Failed to execute flink", e);
        }
    }

    private MySqlSource<String> buildMysqlSource(InputStruct struct) {
        JDBCSource input = (JDBCSource) struct.input();
        String url = input.url();
        String host;
        int port;
        try {
            URIBuilder uriBuilder = new URIBuilder(url.substring(JDBC_PREFIX.length()));
            host = uriBuilder.getHost();
            port = uriBuilder.getPort();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(String.format("Failed to parse url(%s) to get " +
                                                             "hostName and port", url), e);
        }
        return MySqlSource.<String>builder()
                          .hostname(host)
                          .port(port)
                          .databaseList(input.database())
                          .tableList(input.database() + "." + input.table())
                          .username(input.username())
                          .password(input.password())
                          .deserializer(new HugeGraphDeserialization())
                          .build();
    }
}
