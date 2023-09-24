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

package org.apache.hugegraph.spark.connector.utils;

import java.util.List;

import org.apache.hugegraph.spark.connector.mapping.EdgeMapping;
import org.apache.hugegraph.spark.connector.mapping.VertexMapping;
import org.apache.hugegraph.spark.connector.options.HGOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HGUtils {

    private static final Logger LOG = LoggerFactory.getLogger(HGUtils.class);

    public static VertexMapping vertexMappingFromConf(HGOptions hgOptions) {
        String idField = hgOptions.idField();
        VertexMapping vertexMapping = new VertexMapping(idField);

        String label = hgOptions.label();
        vertexMapping.label(label);
        vertexMapping.batchSize(hgOptions.batchSize());
        vertexMapping.selectedFields(hgOptions.selectedFields());
        vertexMapping.ignoredFields(hgOptions.ignoredFields());
        vertexMapping.check();

        // TODO mappingFields, mappingValues, nullValues, updateStrategies
        LOG.info("Update VertexMapping: {}", vertexMapping);
        return vertexMapping;
    }

    public static EdgeMapping edgeMappingFromConf(HGOptions hgOptions) {
        List<String> sourceNames = hgOptions.sourceName();
        List<String> targetNames = hgOptions.targetName();
        EdgeMapping edgeMapping = new EdgeMapping(sourceNames, targetNames);

        String label = hgOptions.label();
        edgeMapping.label(label);
        edgeMapping.batchSize(hgOptions.batchSize());
        edgeMapping.selectedFields(hgOptions.selectedFields());
        edgeMapping.ignoredFields(hgOptions.ignoredFields());
        edgeMapping.check();

        // TODO mappingFields, mappingValues, nullValues, updateStrategies
        LOG.info("Update EdgeMapping: {}", edgeMapping);
        return edgeMapping;
    }
}
