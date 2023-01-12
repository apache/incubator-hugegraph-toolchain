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

package org.apache.hugegraph.formatter.kgdumper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hugegraph.formatter.Formatter;
import org.apache.hugegraph.structure.JsonGraph;

public class DumpKGFormatter implements Formatter {

    private ComputeSign cs;

    // entity dump format："plaint_id value key weight type parent parent_weight
    // child child_weight region region_weight\t\t\t"
    private static final String ENTITY_FORMAT =
            "%s\t%s\t%s\t%s\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t\t\t";
    // mention dump format: "mention descript descript_weight"
    private static final String MENTION_FORMAT = "%s\t%s\t%s";

    public DumpKGFormatter() throws IOException {
        cs = new ComputeSign("./trade_value", "GBK");
    }

    @Override
    public String dump(JsonGraph.JsonVertex vertex) throws Exception {
        switch (vertex.getLabel()) {
            case "entity":
                return this.dumpEntity(vertex);
            case "mention":
                return this.dumpMemtion(vertex);
            default:
                return "";
        }
    }

    private String dumpEntity(JsonGraph.JsonVertex vertex)
                              throws UnsupportedEncodingException {
        if (vertex == null) {
            return "";
        }
        Set<JsonGraph.JsonEdge> edges = vertex.getEdges();
        Map<String, Object> properties = vertex.properties();

        String plainId = (String) properties.get("plain_id");
        String seqPlainId = cs.computeSeqNum(plainId);
        String value = (String) properties.get("value");
        String key = (String) properties.get("key");
        double weight = (double) properties.get("weight");
        int type = (int) properties.get("type");

        List<String> parent = new ArrayList<>();
        List<String> parentWeight = new ArrayList<>();
        List<String> child = new ArrayList<>();
        List<String> childWeight = new ArrayList<>();
        List<String> region = new ArrayList<>();
        List<String> regionWeight = new ArrayList<>();

        for (JsonGraph.JsonEdge edge : edges) {
            if (edge == null) {
                continue;
            }
            if (!vertex.getId().equals(edge.getSource())) {
                continue;
            }

            Map<String, Object> props = edge.properties();
            switch (edge.getLabel()) {
                case "is":
                    parent.add(((String) edge.getTarget()).split(":", 2)[1]);
                    parentWeight.add(props.get("weight").toString());
                    break;
                case "has":
                    child.add(((String) edge.getTarget()).split(":", 2)[1]);
                    childWeight.add(props.get("weight").toString());
                    break;
                case "region":
                    region.add(((String) edge.getTarget()).split(":", 2)[1]);
                    regionWeight.add(props.get("weight").toString());
                    break;
                default:
                    break;
            }
        }
        return String.format(ENTITY_FORMAT,
                             seqPlainId, value, key, weight, type,
                             String.join("|*|", parent),
                             String.join("|*|", parentWeight),
                             String.join("|*|", child),
                             String.join("|*|", childWeight),
                             String.join("|*|", region),
                             String.join("|*|", regionWeight));
    }

    private String dumpMemtion(JsonGraph.JsonVertex vertex)
                               throws UnsupportedEncodingException {
        if (vertex == null) {
            return "";
        }

        Set<JsonGraph.JsonEdge> edges = vertex.getEdges();
        String value = (String) vertex.properties().get("value");

        List<String> descript = new ArrayList<>();
        List<String> descriptWeight = new ArrayList<>();
        for (JsonGraph.JsonEdge edge : edges) {
            if (!vertex.getId().equals(edge.getSource())) {
                continue;
            }
            if (!edge.getLabel().equals("describe")) {
                continue;
            }

            String plainId = ((String) edge.getTarget()).split(":", 2)[1];
            String seqPlainId = cs.computeSeqNum(plainId);
            descript.add(seqPlainId);
            descriptWeight.add(edge.properties().get("confidence").toString());
        }

        return String.format(MENTION_FORMAT, value,
                             String.join("|*|", descript),
                             String.join("|*|", descriptWeight));
    }
}
