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

package org.apache.hugegraph.service.graphs;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.client.api.graph.GraphMetricsAPI;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.query.GremlinController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.entity.enums.AsyncTaskStatus;
import org.apache.hugegraph.entity.enums.ExecuteStatus;
import org.apache.hugegraph.entity.enums.ExecuteType;
import org.apache.hugegraph.entity.graphs.GraphStatisticsEntity;
import org.apache.hugegraph.entity.query.ExecuteHistory;
import org.apache.hugegraph.entity.query.GremlinQuery;
import org.apache.hugegraph.entity.space.BuiltInEntity;
import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.hugegraph.service.algorithm.AsyncTaskService;
import org.apache.hugegraph.service.auth.UserService;
import org.apache.hugegraph.service.load.LoadTaskService;
import org.apache.hugegraph.service.query.ExecuteHistoryService;
import org.apache.hugegraph.service.query.QueryService;
import org.apache.hugegraph.service.schema.SchemaService;
import org.apache.hugegraph.structure.Task;
import org.apache.hugegraph.structure.constant.GraphReadMode;
import org.apache.hugegraph.structure.gremlin.ResultSet;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.HubbleUtil;
import org.apache.hugegraph.util.PageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.hugegraph.util.GremlinUtil.GREMLIN_LOAD_HLM;

@Log4j2
@Service
public class GraphsService {

    @Autowired
    private SchemaService schemaService;

    @Autowired
    UserService userService;
    @Autowired
    private QueryService queryService;
    @Autowired
    private ExecuteHistoryService historyService;
    @Autowired
    private AsyncTaskService asyncTaskService;
    @Autowired
    private LoadTaskService loadTaskService;

    private static final String GRAPH_STORAGE = "v1/graph/%s/%s/g";
    private static final String RUNNING_TASKS = "running_tasks";
    private static final String STATISTICS = "statistics";
    private static final String GREMLIN_STATISTICS_VERTEX =
            "g.V().groupCount().by(label)";
    private static final String GREMLIN_STATISTICS_EDGE =
            "g.E().groupCount().by(label)";
    private static final String GRAPH_HLM = "hlm";
    private static final String GRAPH_COVID19 = "covid19";

    private final ConcurrentHashMap<String, Map<String, Object>> graphStatistics =
            new ConcurrentHashMap<>();

    public Map<String, Object> get(HugeClient client, String graphSpace,
                                   String graph,
                                   Map<String, String> vermeerInfo) {
//        long storage = getStorage(pdClient, graphSpace, graph);
        Map<String, Object> info = new HashMap<>();
        info.putAll(client.graphs().getGraph(graph));
//        info.put("storage", storage);
        if (vermeerInfo.size() != 0) {
            info.put("status", vermeerInfo.get("status"));
            String lastLoadTime = vermeerInfo.get("update_time");
            // todo date format
            info.put("last_load_time", lastLoadTime);
        }
        return info;
    }

    public IPage<Map<String, Object>> queryPage(HugeClient client,
                                                String graphSpace, String uid,
                                                String query, String createTime,
                                                int pageNo, int pageSize,
                                                boolean isVermeerEnabled,
                                                Map<String, Object> vermeerInfo) {
        List<Map<String, Object>> results =
                sortedGraphsProfile(client, graphSpace, query, createTime,
                                    isVermeerEnabled, vermeerInfo);

        for(Map<String, Object> result : results) {
            String graph = result.get("name").toString();
            try {
                result.put("schemaview", schemaService.getSchemaView(
                        client.assignGraph(graphSpace, graph)));
            } catch (Exception e) {
                e.printStackTrace();
                log.info("Schema exception with graph '{}'", graph);
            }
        }

        return PageUtil.page(results, pageNo, pageSize);
    }

    public List<Map<String, Object>> sortedGraphsProfile(HugeClient client,
                                                         String graphSpace,
                                                         String query,
                                                         String createTime,
                                                         boolean isVermeerEnabled,
                                                         Map<String, Object> vermeerInfo) {
        // Get authorized graphs
        List<Map<String, Object>> graphs = client.graphs().listProfile(query);
        log.info("Query all graphs in '{}' ", graphSpace);
        for (Map<String, Object> info : graphs) {
            String name = info.get("name").toString();
            // delete pd.peers info for security
            info.put("pd.peers", "");
            if (vermeerInfo.containsKey(name)) {
                Map<String, Object> brief =
                        (Map<String, Object>) vermeerInfo.get(name);
                info.put("status", brief.get("status").toString());
                info.put("last_load_time",
                         brief.get("last_load_time").toString());
            } else if (isVermeerEnabled) {
                // default info for non-loaded graph
                info.put("status", "created");
                info.put("last_load_time", "");
            } else {
                info.put("status", "");
                info.put("last_load_time", "");
            }

            info.put("storage", info.get("data_size"));
            info.put("statistic", evCount(client, graphSpace, name));
        }

        List<Map<String, Object>> results =
                graphs.stream()
                      .filter((s) -> s.get("create_time").toString()
                                      .compareTo(createTime) > 0)
                      .sorted((graph1, graph2) -> {
                          boolean default1 = (boolean) graph1.get("default");
                          boolean default2 = (boolean) graph2.get("default");

                          if (default1 != default2) {
                              return Boolean.compare(default2, default1);
                          } else if (default1) {
                              Long time1 =
                                      (Long) graph1.get("default_update_time");
                              Long time2 =
                                      (Long) graph2.get("default_update_time");
                              return time1.compareTo(time2);
                          } else {
                              String name1 = graph1.get("name").toString();
                              String name2 = graph2.get("name").toString();
                              return name1.compareTo(name2);
                          }
                      })
                      .collect(Collectors.toList());
        return results;

    }

    public Set<String> listGraphNames(HugeClient client, String graphSpace,
                                      String uid) {

        return ImmutableSet.copyOf(client.graphs().listGraph());
    }

    @Deprecated
    public Map<String, String> create(HugeClient client, String graph,
                                      boolean isAuth, String schemaTemplate) {
        Map<String, String> conf = new HashMap<>();
        if (isAuth) {
            conf.put("gremlin.graph",
                     "com.baidu.hugegraph.auth.HugeFactoryAuthProxy");

        } else {
            conf.put("gremlin.graph", "com.baidu.hugegraph.HugeFactory");
        }
        if (!StringUtils.isEmpty(schemaTemplate)) {
            conf.put("schema.init_template", schemaTemplate);
        }

        conf.put("store", graph);
        // Only for v3.0.0
        conf.put("backend", "hstore");
        conf.put("serializer", "binary");

        return client.graphs().createGraph(graph, JsonUtil.toJson(conf));
    }

    public Map<String, String> create(HugeClient client, String nickname,
                                      String graph, String schemaTemplate) {
        Map<String, String> conf = new HashMap<>();

        conf.put("store", graph);
        // Only for v3.0.0
        conf.put("backend", "hstore");
        conf.put("serializer", "binary");
        // For 3.5.0
        conf.put("task.scheduler_type", "distributed");
        conf.put("nickname", nickname);
        
        if (StringUtils.isNotEmpty(schemaTemplate)) {
            conf.put("schema.init_template", schemaTemplate);
        }

        return client.graphs().createGraph(graph, JsonUtil.toJson(conf));
    }

    public void update(HugeClient client, String nickname,
                       String graph) {
        client.graphs().update(graph, nickname);
    }

    public void truncate(HugeClient client, String graph,
                         boolean isClearSchema, boolean isClearData) {
        if (isClearSchema) {
            client.graphs().clear(graph, true);
        }
        else if (isClearData) {
            client.graphs().clear(graph, false);
        }
    }

    public void setDefault(HugeClient client, String graph) {
        client.graphs().setDefault(graph);
    }

    public void unSetDefault(HugeClient client, String graph) {
        client.graphs().unSetDefault(graph);
    }

    public Map<String, String> getDefault(HugeClient client) {
        return client.graphs().getDefault();
    }

    public void delete(HugeClient client, String graph, String confirmMessage) {

        client.graphs().remove(graph);
    }

    public GraphReadMode graphReadMode(HugeClient client, String graph) {
        return client.graphs().readMode(graph);
    }

    public void graphReadMode(HugeClient client, String graph, String mode) {
        this.checkReadMode(mode);
        // open(0) means mode equal all, close(1) means mode equal OLTP_ONLY
        if ("0".equals(mode)) {client.graphs().readMode(graph, GraphReadMode.ALL);
        }
        else if ("1".equals(mode)){
            client.graphs().readMode(graph, GraphReadMode.OLTP_ONLY);
        }
    }

    public void checkReadMode(String mode) {
        Ex.check("0".equals(mode) || "1".equals(mode),
                "common.read_mode.invalid", mode);
    }

    public Object clone(HugeClient client, Map<String, Object> params) {
        return ImmutableMap.of("task_id",
                               client.graphs().clone(client.getGraphName(),
                                                     params));
    }

    public static long getStorage(RestClient pdClient,
                                  String graphSpace,
                                     String graph) {
        if (pdClient == null) {
            return 0L;
        }
        String path = String.format("v1/graph/%s/%s/g", graphSpace, graph);
        Map<String, Object> result;
        try {
            result = pdClient.get(path).readObject(Map.class);
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            if (data.containsKey("dataSize")) {
                long dataSize = Long.valueOf(data.get("dataSize").toString());
                return dataSize;
            } else {
                return 0L;
            }
        } catch (Exception e) {
            log.info("Fail to request pd to get data of graph {}-{} : {}",
                     graphSpace, graph, e.getMessage());
            return -1L;
        }
    }

    public static boolean isBigGraph(RestClient pdClient, String graphSpace,
                                     String graph) {
        return isBigStorage(getStorage(pdClient, graphSpace, graph));
    }

    public static boolean isBigStorage(long storageKb) {
        return (storageKb > (2 * 1024 * 1024));
    }

    public static String getStatisticsKey(String graphSpace, String graph) {
        return graphSpace + "-" + graph;
    }

    public GraphStatisticsEntity getStatistics( // RestClient pdClient,
                                               HugeClient client,
                                               String graphSpace,
                                               String graph) {
//        long storage = getStorage(pdClient, graphSpace, graph);
//        boolean isBig = isBigStorage(storage);
//        GraphStatisticsEntity result;
//        if (isBig) {
//            result = getLastStatistics(client, graphSpace, graph);
//        } else {
//            this.graphStatistics.clear();
//            result = postSmallStatistics(client, graphSpace, graph);
//        }
//        result.setStorage(storage);

        GraphStatisticsEntity result;
        this.graphStatistics.clear();
        result = postSmallStatistics(client, graphSpace, graph);

        return result;
    }

    public void postStatistics(RestClient pdClient,
                               HugeClient client,
                               String graphSpace,
                               String graph) {
        if (isBigGraph(pdClient, graphSpace, graph)) {
            GremlinQuery query = new GremlinQuery(GREMLIN_STATISTICS_VERTEX);
            long vid = executeAsyncTask(client, graphSpace, graph, query);
            query.setContent(GREMLIN_STATISTICS_EDGE);
            long eid = executeAsyncTask(client, graphSpace, graph, query);
            String idPair = String.valueOf(vid) + "-" + String.valueOf(eid);
            String graphKey = getStatisticsKey(graphSpace, graph);
            if (this.graphStatistics.containsKey(graphKey)) {
                Map<String, Object> graphCache =
                        (Map<String, Object>) this.graphStatistics.get(graphKey);
                if (graphCache.get(RUNNING_TASKS) != null) {
                    List<String> idPairs =
                            (List<String>) graphCache.get(RUNNING_TASKS);
                    idPairs.add(idPair);
                    return ;
                }
            }
            List<String> idPairs = new ArrayList<>();
            idPairs.add(idPair);
            Map<String, Object> graphCache = new HashMap<>(2);
            graphCache.put(RUNNING_TASKS, idPairs);
            graphCache.put(STATISTICS, GraphStatisticsEntity.emptyEntity());
            this.graphStatistics.put(graphKey, graphCache);
        }
    }

    public GraphStatisticsEntity getLastStatistics(HugeClient client,
                                                   String graphSpace,
                                                   String graph) {
        // used for big graph
        String graphKey = getStatisticsKey(graphSpace, graph);
        if (!this.graphStatistics.containsKey(graphKey)) {
            // check graph statistics for the first time
            return GraphStatisticsEntity.emptyEntity();
        }

        Map<String, Object> graphCache =
                (Map<String, Object>) this.graphStatistics.get(graphKey);
        if (graphCache.get(RUNNING_TASKS) != null) {
            List<String> idPairs =
                    (List<String>) graphCache.get(RUNNING_TASKS);
            List<Long> idList = new ArrayList<>(idPairs.size() * 2);
            for (String idPair: idPairs) {
                String[] idVE = idPair.split("-");
                idList.add(Long.valueOf(idVE[0]));
                idList.add(Long.valueOf(idVE[1]));
            }
            List<Task> tasks = asyncTaskService.list(client, idList);
            idList.clear();

            Map<String, Task> taskMap = new HashMap<>(tasks.size());
            for (Task task: tasks) {
                taskMap.put(String.valueOf(task.id()), task);
            }

            List<String> removeIds = new ArrayList<>();
            Task lastV = null;
            Task lastE = null;
            boolean init = true;
            for (String idPair: idPairs) {
                String[] idVE = idPair.split("-");
                Task taskV = taskMap.get(idVE[0]);
                Task taskE = taskMap.get(idVE[1]);
                boolean success = taskV.success() && taskE.success();
                if (removable(taskV) || removable(taskE) || success) {
                    removeIds.add(idPair);
                }

                if (success) {
                    // try to find last updated task
                    if (init) {
                        lastV = taskV;
                        lastE = taskE;
                        init = false;
                    }
                    if (lastV.updateTime() <= taskV.updateTime() &&
                        lastE.updateTime() <= taskE.updateTime()) {
                        lastV = taskV;
                        lastE = taskE;
                    }
                }
            }

            idPairs.removeAll(removeIds);
            removeIds.clear();
            taskMap.clear();

            GraphStatisticsEntity result;
            if (!init) {
                result = updateCacheFromTask(client, lastV, lastE);
            } else {
                result = GraphStatisticsEntity.emptyEntity();
            }
            graphCache.put(STATISTICS, result);
            return result;
        } else if (graphCache.get(STATISTICS) != null) {
            return (GraphStatisticsEntity) graphCache.get(STATISTICS);
        } else {
            GraphStatisticsEntity result = GraphStatisticsEntity.emptyEntity();
            graphCache.put(STATISTICS, result);
            return result;
        }
    }

    public static boolean removable(Task task) {
        return task.completed() && !task.success();
    }

    public GraphStatisticsEntity updateCacheFromTask(HugeClient client,
                                                     Task taskV, Task taskE) {
        GraphStatisticsEntity result = new GraphStatisticsEntity();
        taskV = asyncTaskService.get(client,
                                     Integer.valueOf(
                                             String.valueOf(taskV.id())));
        List<Map<String, Object>> results =
                (List<Map<String, Object>>)
                        JsonUtil.fromJson(taskV.result().toString(),
                                          List.class);
        result.setVertices(results.get(0));
        result.setVertexCount(getCountFromLabels(results.get(0)));

        taskE = asyncTaskService.get(client,
                                     Integer.valueOf(
                                             String.valueOf(taskE.id())));
        results = (List<Map<String, Object>>)
                JsonUtil.fromJson(taskE.result().toString(), List.class);
        result.setEdges(results.get(0));
        result.setEdgeCount(getCountFromLabels(results.get(0)));
        result.setUpdateTime(HubbleUtil.dateFormat());
        return result;
    }

    public GraphStatisticsEntity postSmallStatistics(HugeClient client,
                                                     String graphSpace,
                                                     String graph) {
        GraphStatisticsEntity result = GraphStatisticsEntity.emptyEntity();
        ResultSet vertexResult =
                queryService.executeQueryCount(client,
                                               GREMLIN_STATISTICS_VERTEX);
        ResultSet edgeResult =
                queryService.executeQueryCount(client,
                                               GREMLIN_STATISTICS_EDGE);
        if (vertexResult.data() != null && vertexResult.data().size() != 0) {
            Map<String, Object> vertices =
                    (Map<String, Object>) vertexResult.data().get(0);
            result.setVertices(vertices);
            result.setVertexCount(getCountFromLabels(vertices));
        }
        if (edgeResult.data() != null && edgeResult.data().size() != 0) {
            Map<String, Object> edges =
                    (Map<String, Object>) edgeResult.data().get(0);
            result.setEdges(edges);
            result.setEdgeCount(getCountFromLabels(edges));
        }
        result.setUpdateTime(HubbleUtil.dateFormat());
        return result;
    }

    public String getCountFromLabels(Map<String, Object> labels) {
        Integer count = 0;
        for (Map.Entry<String, Object> entry: labels.entrySet()) {
            count += (Integer) entry.getValue();
        }
        return count.toString();
    }


    public long executeAsyncTask(HugeClient client, String graphSpace,
                                 String graph, GremlinQuery query) {
        this.checkParamsValid(query);

        Date createTime = HubbleUtil.nowDate();
        // Insert execute history
        ExecuteStatus status = ExecuteStatus.ASYNC_TASK_RUNNING;
        ExecuteHistory history;
        history = new ExecuteHistory(null, graphSpace, graph, 0L,
                                     ExecuteType.GREMLIN_ASYNC,
                                     query.getContent(), status,
                                     AsyncTaskStatus.UNKNOWN, -1L, createTime);
        this.historyService.save(history);

        StopWatch timer = StopWatch.createStarted();
        long asyncId = 0L;
        try {
            asyncId = this.queryService.executeGremlinAsyncTask(client, query);
            status = ExecuteStatus.ASYNC_TASK_SUCCESS;
            return asyncId;
        } catch (Throwable e) {
            status = ExecuteStatus.ASYNC_TASK_FAILED;
            throw e;
        } finally {
            timer.stop();
            long duration = timer.getTime(TimeUnit.MILLISECONDS);
            history.setStatus(status);
            history.setDuration(duration);
            history.setAsyncId(asyncId);
            this.historyService.update(history);
        }
    }

    private void checkParamsValid(GremlinQuery query) {
        Ex.check(!org.apache.commons.lang3.StringUtils.isEmpty(query.getContent()),
                 "common.param.cannot-be-null-or-empty",
                 "gremlin-query.content");
        GremlinController.checkContentLength(query.getContent());
    }

    public void initBuiltIn(HugeClient client, GraphConnection connection,
                            BuiltInEntity entity) {
        List<String> graphs = client.graphs().listGraph();
        if (entity.initHlm) {
            initHlm(client, graphs.contains(GRAPH_HLM));
        }

        client.assignGraph(Constant.BUILT_IN, null);
        if (entity.initCovid19) {
            connection.setGraph(GRAPH_COVID19);
            initCovid19(client, graphs.contains(GRAPH_COVID19), connection);
        }
    }

    public void initHlm(HugeClient client, boolean exist) {
        if (!exist) {
            this.create(client, "红楼梦", GRAPH_HLM, null);
        } else {
            this.update(client, "红楼梦", GRAPH_HLM);
            this.truncate(client, GRAPH_HLM, true, false);
        }

        GremlinQuery query = new GremlinQuery(GREMLIN_LOAD_HLM);
        client.assignGraph(Constant.BUILT_IN, GRAPH_HLM);
        this.queryService.executeGremlinQuery(client, query);
    }

    public void initCovid19(HugeClient client, boolean exist,
                            GraphConnection connection) {
        if (!exist) {
            this.create(client, "新冠患者轨迹追溯", GRAPH_COVID19, null);
        } else {
            this.update(client, "新冠患者轨迹追溯", GRAPH_COVID19);
            this.truncate(client, GRAPH_COVID19, true, false);
        }

        // todo load data
        loadTaskService.startCovid19(connection, Constant.BUILT_IN,
                                     GRAPH_COVID19, client);
    }

    /**
     * 统计指定单个图中的顶点总数和边总数
     */
    public Map<String, Object> evCount(HugeClient client,
                                       String graphSpace,
                                       String graph) {
        Map<String, Object> res = new HashMap<>();
        long edgeCount = 0L;
        long vertexCount = 0L;
        String statisticDate = HubbleUtil.dateFormatDay(HubbleUtil.nowDate());
        client.assignGraph(graphSpace, graph);
        GraphMetricsAPI.ElementCount statistic =
                client.graph().getEVCount(statisticDate);
        if (statistic == null) {
            statisticDate = HubbleUtil.dateFormatLastDay();
            statistic = client.graph().getEVCount(statisticDate);
        }

        if (statistic != null) {
            vertexCount = statistic.getVertices();
            edgeCount += statistic.getEdges();
        }

        res.put("date", statisticDate);
        res.put("vertex", vertexCount);
        res.put("edge", edgeCount);
        return res;
    }
}
