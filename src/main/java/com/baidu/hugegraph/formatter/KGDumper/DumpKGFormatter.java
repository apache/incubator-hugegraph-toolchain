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

package com.baidu.hugegraph.formatter.kgdumper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.base.Printer;
import com.baidu.hugegraph.formatter.Formatter;
import com.baidu.hugegraph.structure.JsonGraph.JsonVertex;
import com.baidu.hugegraph.structure.JsonGraph.JsonEdge;

public class DumpKGFormatter implements Formatter {

    private ComputeSign cs;

    // entity dump format："plaint_id value key weight type parent parent_weight
    // child child_weight region region_weight\t\t\t"
    private String entity_format = "%s\t%s\t%s\t%s\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t\t\t";
    // mention dump format: "mention descript descript_weight"
    private String mention_format = "%s\t%s\t%s";

    public DumpKGFormatter() throws IOException {
        cs = new ComputeSign("./trade_value", "GBK");
    }

    @Override
    public String dump(JsonVertex vertex) throws Exception {
        // Printer.print("Dump vertex '%s', its edges size: %d",
        //               vertex.getId(), vertex.getEdges().size());
        switch (vertex.getLabel()) {
            case "entity":
                return this.dumpEntity(vertex);
            case "mention":
                return this.dumpMemtion(vertex);
            default:
                return "";
        }
    }

    private String dumpEntity(JsonVertex vertex)
                              throws UnsupportedEncodingException {
        if (vertex == null) {
            return "";
        }
        List<JsonEdge> edges = vertex.getEdges();
        Map<String, Object> properties = vertex.properties();

        String plainId = (String) properties.get("plain_id");
        String seqPlainId = cs.computeSeqNum(plainId);
        String value = (String) properties.get("value");
        String key = (String) properties.get("key");
        double weight = (double) properties.get("weight");
        int type = (int) properties.get("type");
        List<String> parent = new ArrayList<String>();
        List<String> parentWeight = new ArrayList<String>();
        List<String> child = new ArrayList<String>();
        List<String> childWeight = new ArrayList<String>();
        List<String> region = new ArrayList<String>();
        List<String> regionWeight = new ArrayList<String>();

        for (int i = 0; i < edges.size(); i++) {
            JsonEdge edge = edges.get(i);
            if (edge == null) {
                continue;
            }
            Map<String, Object> props = edge.properties();
            // Printer.print("id:%s source:%s", vertex.getId(), edge.getSource());
            if (!vertex.getId().equals(edge.getSource())) {
                continue;
            }
            // Printer.print("%d label:%s", i, edge.getLabel());
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
            }
        }
        return String.format(entity_format,
                             seqPlainId, value, key, weight, type,
                             String.join("|*|", parent),
                             String.join("|*|", parentWeight),
                             String.join("|*|", child),
                             String.join("|*|", childWeight),
                             String.join("|*|", region),
                             String.join("|*|", regionWeight));
    }

    private String dumpMemtion(JsonVertex vertex)
                               throws UnsupportedEncodingException {
        if (vertex == null) {
            return "";
        }
        List<JsonEdge> edges = vertex.getEdges();
        Map<String, Object> properties = vertex.properties();

        String value1 = (String) properties.get("value");
        List<String> descript = new ArrayList<String>();
        List<String> descriptWeight = new ArrayList<String>();
        // Printer.print("edges size: %d", tmp.size());
        for (int i = 0; i < edges.size(); i++) {
            JsonEdge edge = edges.get(i);
            // Printer.print("id:%s source:%s", vertex.getId(), edge.getSource());
            if (!vertex.getId().equals(edge.getSource())) {
                continue;
            }
            // Printer.print("id:%s source:%s is the same", vertex.getId(), edge.getSource());
            // Printer.print("mention to write vertex: %d", i);

            if (edge.getLabel().equals("describe")) {
                // Printer.print("lable:%s ", edge.getLabel());
                String plainId = ((String) edge.getTarget()).split(":", 2)[1];
                String seqPlainId = cs.computeSeqNum(plainId);
                // Printer.print("lable:%s, plainid:%s, seqPlainId:%s", edge.getLabel(), plainId, seqPlainId);
                descript.add(seqPlainId);
                descriptWeight.add(edge.properties().get("confidence").toString());
            }
        }

        return String.format(mention_format, value1,
                             String.join("|*|", descript),
                             String.join("|*|", descriptWeight));
    }
}
