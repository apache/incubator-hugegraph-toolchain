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

package org.apache.hugegraph.controller.graphs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.hugegraph.entity.graphs.GraphCloneEntity;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.entity.graphs.GraphStatisticsEntity;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.service.space.VermeerService;
import org.apache.hugegraph.structure.constant.GraphReadMode;
import com.google.common.collect.ImmutableMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.service.graphs.GraphsService;
import org.apache.hugegraph.service.load.JobManagerService;
import org.apache.hugegraph.service.query.ExecuteHistoryService;
import org.apache.hugegraph.service.query.GremlinCollectionService;
import org.apache.hugegraph.service.space.SchemaTemplateService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs")
public class GraphsController extends BaseController {

    @Autowired
    GraphsService graphsService;
    @Autowired
    SchemaTemplateService schemaTemplateService;
    @Autowired
    ExecuteHistoryService executeHistoryService;
    @Autowired
    GremlinCollectionService gremlinCollectionService;
    @Autowired
    JobManagerService jobManagerService;
    @Autowired
    VermeerService vermeerService;
    @Autowired
    HugeConfig config;

    public boolean isVermeerEnabled() {
        String username = this.getUser();
        String password = (String) this.getSession("password");
        return vermeerService.isVermeerEnabled(username, password);
    }

    public String getGraphFromVermeer(String vermeer) {
        return vermeer.split("-")[1];
    }

    private Map<String, Object> getVermeerGraphs(String graphspace,
                                                 boolean enable) {
        if (enable) {
            HugeClient client = this.authClient(graphspace, null);
            List<Map<String, Object>> graphinfos;
            try {
                graphinfos = (List<Map<String, Object>>) client.vermeer()
                                                               .getGraphsInfo()
                                                               .get("graphs");
            } catch (ServerException e) {
                log.info("无法连接到vermeer平台", e.cause());
                graphinfos = null;
            }
            if (graphinfos == null || graphinfos.size() == 0) {
                // if vermeer data cleared
                return ImmutableMap.of();
            }
            String prefix = graphspace + "-";
            graphinfos = graphinfos.stream().filter((g) -> g.get("name")
                                               .toString().startsWith(prefix))
                                   .collect(Collectors.toList());
            Map<String, Object> briefs = new HashMap<>(graphinfos.size());
            for (Map<String, Object> info: graphinfos) {
                String name = getGraphFromVermeer(info.get("name").toString());
                Map<String, Object> brief = new HashMap<>();
                brief.put("name", name);
                brief.put("status", info.get("status").toString());

                String lastLoadTime = info.get("update_time").toString();
                // todo date format
                brief.put("last_load_time", lastLoadTime);
                briefs.put(name, brief);
            }
            return briefs;
        }
        return ImmutableMap.of();
    }

    private Map<String, String> getVermeerGraph(String graphspace,
                                                String graph) {
        boolean enable = isVermeerEnabled();
        if (enable) {
            String prefix = graphspace + "-";
            Map<String, Object> graphInfo = null;
            try {
                HugeClient client = this.authClient(graphspace, null);

                graphInfo = (Map<String, Object>) client.vermeer()
                                                        .getGraphInfoByName(prefix + graph)
                                                        .get("graph");
            } catch (ServerException e) {
                // if dashboard enables vermeer but server sets wrong vermeer 
                // address, return null
                log.info("无法连接到vermeer平台", e.cause());
                return ImmutableMap.of();
            }

            if (graphInfo == null || graphInfo.isEmpty()) {
                return ImmutableMap.of();
            }

            Map<String, String> brief = new HashMap<>();

            String name = getGraphFromVermeer(graphInfo.get("name").toString());
            brief.put("name", name);
            brief.put("status", graphInfo.get("status").toString());
            String lastLoadTime = graphInfo.get("update_time").toString();
            // todo date format
            brief.put("last_load_time", lastLoadTime);
            return brief;
        }
        return ImmutableMap.of();
    }

    @GetMapping("list")
    public Object listGraphs(@PathVariable("graphspace") String graphspace) {
        // Get list of authorized graphs
        HugeClient client = this.authClient(graphspace, null);

        boolean enable = isVermeerEnabled();
        Map<String, Object> vermeerInfo = getVermeerGraphs(graphspace, enable);
        List<Map<String, Object>> sortedGraphs =
                graphsService.sortedGraphsProfile(client, graphspace, "", "",
                                                  enable, vermeerInfo);

        return ImmutableMap.of("graphs", sortedGraphs);
    }

    @GetMapping
    public Object queryPage(@PathVariable("graphspace") String graphspace,
                            @RequestParam(name = "query", required = false,
                                    defaultValue = "") String query,
                            @RequestParam(name = "create_time", required = false,
                                    defaultValue = "") String createTime,
                            @RequestParam(name = "page_no", required = false,
                                    defaultValue = "1") int pageNo,
                            @RequestParam(name = "page_size", required = false,
                                    defaultValue = "10") int pageSize) {
        HugeClient client = this.authClient(graphspace, null);
        boolean enable = isVermeerEnabled();
        Map<String, Object> vermeerInfo = getVermeerGraphs(graphspace, enable);
        Object result = this.graphsService.queryPage(client, graphspace,
                                                     getUser(), query,
                                                     createTime, pageNo,
                                                     pageSize, enable,
                                                     vermeerInfo);

        return result;
    }

    @GetMapping("{graph}/get")
    public Object get(@PathVariable("graphspace") String graphspace,
                      @PathVariable("graph") String graph) {
        Map<String, String> vermeerInfo = getVermeerGraph(graphspace, graph);
        Object result = graphsService.get(this.authClient(graphspace, graph),
                                          graphspace, graph, vermeerInfo);
        return result;
    }

    // no-use
//    @GetMapping("{graph}/storage")
//    public Map<String, Object> storage(@PathVariable("graphspace") String graphspace,
//                      @PathVariable("graph") String graph) {
//        RestClient pdClient = getPdClient();
//        Map<String, Object> result = ImmutableMap.of("storage",
//                                                     String.valueOf(
//                                       graphsService.getStorage(graphspace,
//                                                                graph)));
//        if (pdClient != null) {
//            pdClient.close();
//        }
//        return result;
//    }

    @PostMapping("{graph}/statistics")
    public void postStatistics(
            @PathVariable("graphspace") String graphspace,
            @PathVariable("graph") String graph) {
        HugeClient client = this.authClient(graphspace, graph);
//        RestClient pdClient = getPdClient();
        graphsService.postStatistics(null, client, graphspace, graph);
//        if (pdClient != null) {
//            pdClient.close();
//        }
    }

    @GetMapping("{graph}/statistics")
    public GraphStatisticsEntity getStatistics(
            @PathVariable("graphspace") String graphspace,
            @PathVariable("graph") String graph) {
        HugeClient client = this.authClient(graphspace, graph);
//        RestClient pdClient = getPdClient();
        GraphStatisticsEntity result =
                graphsService.getStatistics(client, graphspace,
                                            graph);
//        if (pdClient != null) {
//            pdClient.close();
//        }
        return result;
    }

    @PostMapping
    public Object create(@PathVariable("graphspace") String graphspace,
                         @RequestParam("graph") String graph,
                         @RequestParam("nickname") String nickname,
                         @RequestParam(value = "auth", required = false,
                                 defaultValue = "false") boolean isAuth,
                         @RequestParam(value = "schema", required = false)
                                 String schemaTemplage) {

        return this.graphsService.create(this.authClient(graphspace, null), 
                                         nickname, graph, schemaTemplage);
    }

    @GetMapping("{graph}/update")
    public void update(@PathVariable("graphspace") String graphspace,
                       @PathVariable("graph") String graph,
                       @RequestParam("nickname") String nickname) {

        this.graphsService.update(this.authClient(graphspace, null),
                nickname, graph);
    }

    @DeleteMapping("{graph}")
    public void delete(@PathVariable("graphspace") String graphspace,
                       @PathVariable("graph") String graph,
                       @RequestParam(value = "message", required = false)
                               String message) {

        Map<String, String> vermeer = getVermeerGraph(graphspace, graph);
        if (!vermeer.isEmpty()) {
            HugeClient client = this.authClient(null, null);
            try {
                vermeerService.deleteGraph(client, graphspace, graph);
            } catch (Exception e) {
                // HugeGraph-Common assert request with delete method returns
                // 202 or 204
                if (!e.getMessage().contains("deleted ok")) {
                    throw new ExternalException("delete vermeer graph error " +
                                                "%s", e.getMessage());
                }
            }
        }

        this.graphsService.delete(this.authClient(graphspace, graph), graph,
                                  message);

        // Clean Local DB Data
        executeHistoryService.deleteByGraph(graphspace, graph);
        gremlinCollectionService.deleteByGraph(graphspace, graph);
        jobManagerService.removeByGraph(graphspace, graph);
    }


    @GetMapping("{graph}/truncate")
    public void truncate(@PathVariable("graphspace") String graphspace,
                         @PathVariable("graph") String graph,
                         @RequestParam(value = "clear_schema", required = false,
                                 defaultValue = "false") boolean isClearSchema,
                         @RequestParam(value = "clear_data", required = false,
                                 defaultValue = "false") boolean isClearData) {
        this.graphsService.truncate(this.authClient(graphspace, graph), graph,
                isClearSchema, isClearData);
    }

    @GetMapping("{graph}/setdefault")
    public void setDefault(@PathVariable("graphspace") String graphspace,
                           @PathVariable("graph") String graph) {
        this.graphsService.setDefault(this.authClient(graphspace, graph), graph);
    }

    @GetMapping("{graph}/unsetdefault")
    public void unSetDefault(@PathVariable("graphspace") String graphspace,
                             @PathVariable("graph") String graph) {
        this.graphsService.unSetDefault(this.authClient(graphspace, graph),
                                        graph);
    }

    @GetMapping("getdefault")
    public Map<String, String> getDefault(@PathVariable("graphspace") String graphspace) {
        return this.graphsService.getDefault(this.authClient(graphspace, null));
    }

    @PutMapping("{graph}/graph_read_mode")
    public void graphReadMode(@PathVariable("graphspace") String graphspace,
                                @PathVariable("graph") String graph,
                                @RequestBody String mode) {
        this.graphsService.graphReadMode(this.authClient(graphspace, graph), graph, mode);
    }

    @GetMapping("{graph}/graph_read_mode")
    public Map<String, String> graphReadMode(@PathVariable("graphspace") String graphspace,
                                               @PathVariable("graph") String graph) {
        Map<String, String> status = new HashMap<>();
        GraphReadMode graphMode = this.graphsService.graphReadMode(this.authClient(graphspace, graph), graph);
        if ("all".equals(graphMode.string())) {
            status.put("status", "0");
        }
        else if ("oltp_only".equals(graphMode.string())) {
            status.put("status", "1");
        }
        return status;
    }

    @PostMapping("{graph}/clone")
    public Object clone(@PathVariable("graphspace") String graphspace,
                        @PathVariable("graph") String graph,
                        @RequestBody GraphCloneEntity graphCloneEntity) {
        // TODO X clean code at here
        //HugeClient clientOfDefaultGs = this.authClient("DEFAULT", "");
        // clone request must post to default server
        //clientOfDefaultGs.assignGraph(graphspace, graph);
        //return this.graphsService.clone(clientOfDefaultGs, graphCloneEntity.convertMap());
        return this.graphsService.clone(this.authClient(graphspace, graph),
                                        graphCloneEntity.convertMap(graphspace,
                                                                    graph));
    }
    //
    //@Data
    //@NoArgsConstructor
    //@AllArgsConstructor
    //@Builder
    //private static class GraphCloneEntity {
    //    @JsonProperty("target_graphspace")
    //    public String targetGraphSpace;
    //
    //    @JsonProperty("target_graph")
    //    public String targetGraph;
    //
    //    public Map<String, Object> convertMap() {
    //        return Map.of("target_graphspace", targetGraphSpace,
    //                      "target_graph", targetGraph);
    //    }
    //}
}
