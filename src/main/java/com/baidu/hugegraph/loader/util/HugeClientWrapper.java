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

package com.baidu.hugegraph.loader.util;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.loader.executor.LoadOptions;

public final class HugeClientWrapper {

    private static volatile HugeClient instance;

    public static HugeClient get(LoadOptions options) {
        if (instance == null) {
            synchronized(HugeClientWrapper.class) {
                if (instance == null) {
                    instance = newHugeClient(options);
                }
            }
        }
        return instance;
    }

    private HugeClientWrapper() {}

    private static HugeClient newHugeClient(LoadOptions options) {
        String address = options.host + ":" + options.port;
        if (options.token == null) {
            return new HugeClient(address, options.graph, options.timeout,
                                  options.maxConnections,
                                  options.maxConnectionsPerRoute);
        } else {
            // The username is same as graph name
            return new HugeClient(address, options.graph, options.graph,
                                  options.token, options.timeout,
                                  options.maxConnections,
                                  options.maxConnectionsPerRoute);
        }
    }

//    Maybe make it unnecessary to distinguish vertex and edge in taskmanager
//    @SuppressWarnings("unchecked")
//    public static <GE extends GraphElement> List<GE> addBatch(ElemType type,
//                                                              List<GE> elements,
//                                                              boolean check) {
//        GraphManager graph = instance.graph();
//        if (type.isVertex()) {
//            return (List<GE>) graph.addVertices((List<Vertex>) elements);
//        } else {
//            assert type.isEdge();
//            return (List<GE>) graph.addEdges((List<Edge>) elements, check);
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    public static <GE extends GraphElement> GE addSingle(ElemType type,
//                                                         GE element) {
//        GraphManager graph = instance.graph();
//        if (type.isVertex()) {
//            return (GE) graph.addVertex((Vertex) element);
//        } else {
//            assert type.isEdge();
//            return (GE) graph.addEdge((Edge) element);
//        }
//    }
}
