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

package org.apache.hugegraph.controller.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.query.ElementEditHistory;
import org.apache.hugegraph.entity.query.GremlinQuery;
import org.apache.hugegraph.entity.query.GremlinResult;
import org.apache.hugegraph.service.query.EditElementHistoryService;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.SchemaElement;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.util.PageUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs" +
                "/{graph}/edit-histories")
public class EditElementHistoryController extends BaseController {

    @Autowired
    private EditElementHistoryService service;

    @Autowired
    private GremlinQueryController gremlinService;

    /**
     * 根据条件查询编辑历史记录
     * @param graphSpace     图空间
     * @param graph          图
     * @param optionPersons  操作人
     * @param optionTimeFrom 操作时间from
     * @param optionTimeTo   操作时间to
     * @param optionTypes    操作类型
     * @param elementId      元素id
     * @param pageNo         页码
     * @param pageSize       每页大小
     * @return 返回分页后的编辑历史记录
     */
    @GetMapping("filter")
    public IPage<ElementEditHistory> queryByConditions(
            @PathVariable("graphspace") String graphSpace,
            @PathVariable("graph") String graph,
            @RequestParam(name = "option_person",
                          required = false,
                          defaultValue = "") String optionPersons,
            @RequestParam(name = "option_time_from",
                          required = false,
                          defaultValue = "") String optionTimeFrom,
            @RequestParam(name = "option_time_to",
                          required = false,
                          defaultValue = "") String optionTimeTo,
            @RequestParam(name = "option_type",
                          required = false,
                          defaultValue = "") String optionTypes,
            @RequestParam(name = "element_id",
                          required = false,
                          defaultValue = "") String elementId,
            @RequestParam(name = "page_no",
                          required = false,
                          defaultValue = "1") int pageNo,
            @RequestParam(name = "page_size",
                          required = false,
                          defaultValue = "10") int pageSize) {
        boolean hasNoConditions =
                hasNoConditions(optionPersons, optionTimeFrom, optionTimeTo,
                              optionTypes, elementId);
        if (hasNoConditions) {
            return this.service.list(graphSpace, graph, pageNo, pageSize);
        }

        List<String> optionPersonsList =
                optionPersons.isEmpty() ? new ArrayList<>() :
                Arrays.asList(optionPersons.split(","));

        List<String> optionTypesList =
                optionTypes.isEmpty() ? new ArrayList<>() :
                Arrays.asList(optionTypes.split(","));

        return this.service.queryByConditions(graphSpace, graph,
                                              optionPersonsList,
                                              optionTypesList,
                                              elementId,
                                              optionTimeFrom,
                                              optionTimeTo,
                                              pageNo, pageSize);
    }

    @GetMapping("gremlin")
    public IPage<Element> queryByGremlin(
            @PathVariable("graphspace") String graphSpace,
            @PathVariable("graph") String graph,
            @RequestParam(name = "page_no",
                          required = false,
                          defaultValue = "1") int pageNo,
            @RequestParam(name = "page_size",
                          required = false,
                          defaultValue = "10") int pageSize,
            @RequestParam(name = "gremlin_query",
                          required = true) String gremlinQuery) {
        GremlinResult gremlin =
                gremlinService.gremlin(graphSpace, graph,
                                       new GremlinQuery(gremlinQuery));
        List<GraphElement> elements = collectElements(gremlin);
        if (elements.isEmpty()) {
            return PageUtil.page(new ArrayList<>(), pageNo, pageSize);
        }

        List<GraphElement> subElements = getSubList(elements, pageNo, pageSize);

        List<String> elementIds = new ArrayList<>();
        subElements.forEach(e -> elementIds.add(e.id().toString()));

        Map<String, ElementEditHistory> histories =
                service.queryByElementIds(graphSpace, graph, elementIds);

        List<Element> mergedElements =
                mergeEditHistory(this.authClient(graphSpace, graph), histories,
                                 elements);
        return PageUtil.page(mergedElements, pageNo, pageSize);
    }

    private List<Element> mergeEditHistory(HugeClient client,
                                           Map<String, ElementEditHistory> histories,
                                           List<GraphElement> elements) {
        List<Element> mergedElements = new ArrayList<>();

        Map<String, VertexLabel> vlMap = new HashMap<>();
        client.schema().getVertexLabels()
              .forEach(vl -> vlMap.put(vl.name(), vl));

        Map<String, EdgeLabel> elMap = new HashMap<>();
        client.schema().getEdgeLabels()
              .forEach(el -> elMap.put(el.name(), el));


        for (GraphElement e : elements) {
            Element.ElementBuilder eBuilder =
                    Element.builder()
                           .element(e)
                           .propertyNum(e.properties().size());
            if (e instanceof Vertex) {
                eBuilder.label(vlMap.get(e.label()));
            } else if (e instanceof Edge) {
                eBuilder.label(elMap.get(e.label()));
            }
            ElementEditHistory his = histories.get(e.id().toString());
            if (his != null) {
                eBuilder.lastContent(his.getContent())
                        .lastOptionTime(his.getOptionTime())
                        .lastOptionPerson(his.getOptionPerson())
                        .lastOptionType(his.getOptionType());
            }
            mergedElements.add(eBuilder.build());
        }
        return mergedElements;
    }

    private List<GraphElement> getSubList(List<GraphElement> elements,
                                          int pageNo, int pageSize) {
        int start = Math.min((pageNo - 1) * pageSize, elements.size());
        int end = Math.min(pageNo * pageSize, elements.size());
        return elements.subList(start, end);
    }


    private List<GraphElement> collectElements(GremlinResult gremlin) {
        List<GraphElement> elements = new ArrayList<>();
        if (gremlin.getType().isEmpty()) {
            return elements;
        }

        for (Object obj : gremlin.getJsonView().getData()) {
            if (obj instanceof Vertex) {
                Vertex v = (Vertex) obj;
                elements.add(v);
            } else if (obj instanceof Edge) {
                Edge e = (Edge) obj;
                elements.add(e);
            }
        }
        return elements;
    }


    private boolean hasNoConditions(String optionPersons,
                                  String optionTimeFrom,
                                  String optionTimeTo,
                                  String optionTypes,
                                  String elementId) {
        return optionPersons.isEmpty() && optionTimeFrom.isEmpty()
               && optionTimeTo.isEmpty() && optionTypes.isEmpty() &&
               elementId.isEmpty();
    }


    @Data
    @Builder
    public static class Element {
        @JsonProperty("element")
        private GraphElement element;

        @JsonProperty("label")
        private SchemaElement label;

        @JsonProperty("property_num")
        private int propertyNum;

        @JsonProperty("last_option_type")
        private String lastOptionType;

        @JsonProperty("last_option_time")
        private Date lastOptionTime;

        @JsonProperty("last_option_person")
        private String lastOptionPerson;

        @JsonProperty("last_content")
        private String lastContent;
    }
}
