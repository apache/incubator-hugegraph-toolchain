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

package org.apache.hugegraph.loader.direct.loader;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.apache.hugegraph.loader.builder.EdgeBuilder;
import org.apache.hugegraph.loader.builder.ElementBuilder;
import org.apache.hugegraph.loader.builder.VertexBuilder;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.EdgeMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.VertexMapping;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

public abstract class DirectLoader<T, R> implements Serializable {

    LoadOptions loadOptions;
    InputStruct struct;

    public DirectLoader(LoadOptions loadOptions,
                        InputStruct struct) {
        this.loadOptions = loadOptions;
        this.struct = struct;
    }

    public final void bulkload(Dataset<Row> ds) {
        JavaPairRDD<T, R> javaPairRDD = buildVertexAndEdge(ds);
        String path = generateFiles(javaPairRDD);
        loadFiles(path);
    }

    protected List<ElementBuilder> getElementBuilders() {
        LoadContext context = new LoadContext(loadOptions);
        context.schemaCache().updateAll();
        List<ElementBuilder> buildersForGraphElement = new LinkedList<>();
        for (VertexMapping vertexMapping : struct.vertices()) {
            buildersForGraphElement.add(new VertexBuilder(context, struct, vertexMapping));
        }
        for (EdgeMapping edgeMapping : struct.edges()) {
            buildersForGraphElement.add(new EdgeBuilder(context, struct, edgeMapping));
        }
        context.close();
        return buildersForGraphElement;
    }

    abstract JavaPairRDD<T, R> buildVertexAndEdge(Dataset<Row> ds);

    abstract String generateFiles(JavaPairRDD<T, R> buildAndSerRdd);

    abstract void loadFiles(String path);
}
