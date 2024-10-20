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

package org.apache.hugegraph.service.space;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.hugegraph.client.api.task.TasksWithPage;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.space.GraphSpaceEntity;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.service.auth.UserService;
import org.apache.hugegraph.service.graphs.GraphsService;
import org.apache.hugegraph.structure.Task;
import org.apache.hugegraph.structure.space.GraphSpace;
import org.apache.hugegraph.structure.space.SchemaTemplate;
import org.apache.hugegraph.util.GremlinUtil;
import org.apache.hugegraph.util.HubbleUtil;
import org.apache.hugegraph.util.Log;
import org.apache.hugegraph.util.PageUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphSpaceService {

    private static final Logger LOG = Log.logger(GraphSpaceService.class);

    @Autowired
    private UserService userService;

    @Autowired
    private GraphsService graphsService;

    /**
     * 统计所有图空间总数、图总数 统计顶点总数和边总数、顶点类型及边类型总数、前一天执行任务个数
     *
     * @param client hugegraph-client
     * @return 统计结果集
     */
    public Map<String, Long> metrics(HugeClient client) {
        long gsCount = 0L;
        long gCount = 0L;
        long vCount = 0L;
        long eCount = 0L;
        long vlCount = 0L;
        long elCount = 0L;
        long preDayTaskCount = 0L;
        try {
            List<String> graphSpaces = this.listAll(client);
            gsCount = graphSpaces.size();

            for (String gs : graphSpaces) {
                Map<String, Object> ev = evCount(client, gs);
                Map<String, Object> elVl = elAndVlCount(client, gs);
                Map<String, Object> task = preDayTaskCount(client, gs);

                vCount += (Long) ev.get("vertex");
                eCount += (Long) ev.get("edge");
                vlCount += (Long) elVl.get("vertexlabel");
                elCount += (Long) elVl.get("edgelabel");
                preDayTaskCount += (Long) task.get("task");

                gCount += Long.valueOf(
                        graphsService.listGraphNames(client, gs, "").size());
            }
        } catch (Exception e) {
            LOG.error("Failed to get saas metrics, caused by");
            e.printStackTrace();
            throw e;
        }

        HashMap<String, Long> res = new HashMap<>();
        res.put("gsCount", gsCount);
        res.put("gCount", gCount);
        res.put("vCount", vCount);
        res.put("eCount", eCount);
        res.put("vlCount", vlCount);
        res.put("elCount", elCount);
        res.put("preDayTaskCount", preDayTaskCount);
        return res;
    }


    public IPage<Map<String, Object>> queryPage(HugeClient client, String query,
                                       String createTime, int pageNo, int pageSize) {
        List<Map<String, Object>> results =
                queryAllGs(client, query, createTime);
        return PageUtil.page(results, pageNo, pageSize);
    }

    public List<Map<String, Object>> queryAllGs(HugeClient client, String query,
                                                String createTime) {
        List<Map<String, Object>> results =
                client.graphSpace().listProfile(query).stream()
                      .filter((s) -> s.get("create_time").toString()
                                      .compareTo(createTime) > 0)
                      .collect(Collectors.toList());
        // 将DEFAULT和neizhianli的图空间排在前面, 其他图空间按字母序排序
        Collections.sort(results,
                         (a, b) -> new BuiltInFirst().compare(a.get("name").toString(),
                                                              b.get("name").toString()));
        for (Map<String, Object> info : results) {
            String name = info.get("name").toString();
            info.put("graphspace_admin",
                     userService.listGraphSpaceAdmin(client, name));
            Map<String, Object> statisticTotal = evCount(client, name);
            info.put("statistic", statisticTotal);
        }
        return results;
    }

    /**
     * 统计指定图空间下的顶点总数和边总数
     * @param client
     * @param graphSpace
     * @return
     */
    private Map<String, Object> evCount(HugeClient client, String graphSpace) {
        long vertexTotal = 0L;
        long edgeTotal = 0L;
        Map<String, Object> statisticTotal = new HashMap<>();
        client.assignGraph(graphSpace, "");
        Set<String> graphs = graphsService.listGraphNames(client, graphSpace, "");
        String statisticDate = HubbleUtil.dateFormatDay(HubbleUtil.nowDate());
        for (String graph : graphs) {
            Map<String, Object> graphEvCount =
                    graphsService.evCount(client, graphSpace, graph);

            vertexTotal += (long) graphEvCount.get("vertex");
            edgeTotal += (long) graphEvCount.get("edge");
        }
        statisticTotal.put("date", HubbleUtil.dateFormatDay(statisticDate));
        statisticTotal.put("vertex", vertexTotal);
        statisticTotal.put("edge", edgeTotal);
        return statisticTotal;
    }


    /**
     * 统计指定图空间下的edgeLabel总数和vertexLabel边总数
     * @param client
     * @param graphSpace
     * @return
     */
    private Map<String, Object> elAndVlCount(HugeClient client,
                                             String graphSpace) {
        Map<String, Object> result = new HashMap<>();
        Set<String> graphs =
                graphsService.listGraphNames(client, graphSpace, "");
        long vlCount = 0L;
        long elCount = 0L;
        for (String graph : graphs) {
            client.assignGraph(graphSpace, graph);
            vlCount += client.schema().getVertexLabels().size();
            elCount += client.schema().getEdgeLabels().size();
        }

        result.put("vertexlabel", vlCount);
        result.put("edgelabel", elCount);
        return result;
    }

    /**
     * 前一天的执行任务数
     *
     * @param client
     * @param graphSpace
     * @return
     */
    private Map<String, Object> preDayTaskCount(HugeClient client,
                                                String graphSpace) {
        LOG.info("Task count in [{}]", graphSpace);
        String lastDay = HubbleUtil.dateFormatLastDay();
        Date start = null;
        Date end = null;
        try {
            SimpleDateFormat simpleDateFormat =
                    new SimpleDateFormat("yyyyMMdd HH:mm:ss");
            start = simpleDateFormat.parse(lastDay + " 00:00:00");
            end = simpleDateFormat.parse(lastDay + " 23:59:59");
        } catch (ParseException e) {
            throw new InternalException("parse date error" + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        Set<String> graphs =
                graphsService.listGraphNames(client, graphSpace, "");
        long preDayTaskCount = 0L;
        for (String graph : graphs) {
            client.assignGraph(graphSpace, graph);
            String page = "";
            while (page != null) {
                TasksWithPage pageTask =
                        client.task().list(null, page, 1000);
                for (Task task : pageTask.tasks()) {
                    if (task.createTime() >= start.getTime() &&
                        task.createTime() <= end.getTime()) {
                        preDayTaskCount += 1;
                    }
                }
                page = pageTask.page();
            }
        }
        result.put("task", preDayTaskCount);
        return result;
    }

    public boolean isAuth(HugeClient client, String graphSpace) {
        GraphSpace space = getWithoutAdmins(client, graphSpace);

        return space.isAuth();
    }

    public List<String> listAll(HugeClient client) {
        List<String> result = client.graphSpace().listGraphSpace().stream()
                                    .collect(Collectors.toList());
        Collections.sort(result, (a, b) -> new BuiltInFirst().compare(a, b));
        return result;
    }

    public GraphSpace getWithoutAdmins(HugeClient authClient,
                                       String graphspace) {
        GraphSpace space = authClient.graphSpace().getGraphSpace(graphspace);
        if (space == null) {
            throw new InternalException("graphspace.get.{} Not Exits",
                                        graphspace);
        }

        return space;
    }

    public GraphSpaceEntity getWithAdmins(HugeClient authClient, String graphspace) {
        GraphSpace space = authClient.graphSpace().getGraphSpace(graphspace);
        if (space == null) {
            throw new InternalException("graphspace.get.{} Not Exits",
                                        graphspace);
        }

        GraphSpaceEntity graphSpaceEntity
                = GraphSpaceEntity.fromGraphSpace(space);

        if (authClient.auth().isSuperAdmin()) {
            graphSpaceEntity.graphspaceAdmin =
                    userService.listGraphSpaceAdmin(authClient, graphspace);
        }
        graphSpaceEntity.setStatistic(evCount(authClient, graphspace));

        return graphSpaceEntity;
    }

    public void delete(HugeClient authClient, String graphspace) {
        authClient.graphSpace()
                  .deleteGraphSpace(graphspace);
    }

    public Object create(HugeClient authClient, GraphSpace graphSpace) {
        return authClient.graphSpace().createGraphSpace(graphSpace);
    }

    public GraphSpace update(HugeClient authClient, GraphSpace graphSpace) {
        return authClient.graphSpace().updateGraphSpace(graphSpace);
    }

    public Map<String, String> setdefault(HugeClient authClient, String graphSpace) {
        return authClient.graphSpace().setDefault(graphSpace);
    }

    public Map<String, String> getdefault(HugeClient authClient) {
        return authClient.graphSpace().getDefault();
    }

    public void initBuiltIn(HugeClient client) {
        String builtInStr = Constant.BUILT_IN;
        GraphSpace builtIn = new GraphSpace(builtInStr);
        builtIn.setNickname("内置案例");
        builtIn.setDescription("内置案例");
        builtIn.setMaxGraphNumber(100);
        builtIn.setCpuLimit(1000);
        builtIn.setStorageLimit(1000);
        builtIn.setMemoryLimit(1024);
        builtIn.setOlapNamespace("built_in");
        builtIn.setOltpNamespace("built_in");

        List<String> spaces = client.graphSpace().listGraphSpace();
        if (spaces.contains(builtInStr)) {
            client.graphSpace().deleteGraphSpace(builtInStr);
        }
        client.graphSpace().createGraphSpace(builtIn);

        client.assignGraph(builtInStr, null);
        SchemaTemplate schemaTemplate = new SchemaTemplate("hlm",
                                                           GremlinUtil.GREMLIN_HLM_SCHEMA);
        client.schemaTemplateManager().createSchemaTemplate(schemaTemplate);

        schemaTemplate = new SchemaTemplate("covid19",
                                            GremlinUtil.GREMLIN_COVID19_SCHEMA);
        client.schemaTemplateManager().createSchemaTemplate(schemaTemplate);
    }

    private class BuiltInFirst implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            if (Constant.BUILT_IN.equals(o2)) {
                return 1;
            } else if (Constant.BUILT_IN.equals(o1)) {
                return -1;
            }
            return o1.compareTo(o2);
        }
    }
}
