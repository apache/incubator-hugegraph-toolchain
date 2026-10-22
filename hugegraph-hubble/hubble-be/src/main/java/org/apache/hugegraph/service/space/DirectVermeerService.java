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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ForbiddenException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.auth.AuthModeService;
import org.apache.hugegraph.structure.Task;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.HubbleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DirectVermeerService {

    @Autowired
    private VermeerClient backend;
    @Autowired
    private HugeConfig config;

    @Autowired
    private AuthModeService authMode;

    private final ObjectMapper mapper = new ObjectMapper();

    public boolean configured() {
        return this.backend.configured();
    }

    public boolean permitted(HugeClient client) {
        return this.authMode.anonymous() || client.auth().isSuperAdmin();
    }

    public boolean available() {
        if (!this.configured()) {
            return false;
        }
        try {
            // Graphs requires authentication; healthcheck alone does not.
            this.backend.request("GET", "graphs", null);
            Map<String, Object> workers = this.backend.request("GET", "workers", null);
            List<?> active = HubbleUtil.uncheckedCast(workers.get("workers"));
            return active != null && !active.isEmpty();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public Map<String, Object> graphs() {
        return this.backend.request("GET", "graphs", null);
    }

    public Map<String, Object> graph(String graphspace, String graph) {
        String name = name(graphspace, graph);
        List<Map<String, Object>> graphs = HubbleUtil.uncheckedCast(
                this.graphs().getOrDefault("graphs", List.of()));
        for (Map<String, Object> item : graphs) {
            if (name.equals(item.get("name"))) {
                return Map.of("graph", item);
            }
        }
        return Map.of();
    }

    public Map<String, Object> load(HugeClient client, boolean force,
                                    Map<String, Object> options) {
        authorize(client);
        Map<String, Object> graph = HubbleUtil.uncheckedCast(
                this.graph(client.getGraphSpaceName(), client.getGraphName()).get("graph"));
        if (graph != null && "error".equals(graph.get("status"))) {
            // A failed initial load leaves a graph that native load/reload both
            // reject. Unload that failed graph so the user can retry loading.
            this.deleteGraph(client);
            graph = null;
        }
        String taskType = graph == null ? "load" : force ? "reload" : "load";
        Map<String, String> params = new HashMap<>();
        // Only loader tuning is user-controlled. Targets and credentials are not.
        for (String key : List.of("load.parallel", "load.use_outedge",
                                  "load.use_out_degree", "load.use_property")) {
            if (options != null && options.get(key) != null) {
                params.put(key, options.get(key).toString());
            }
        }
        params.putIfAbsent("load.parallel", "4");
        params.putIfAbsent("load.use_outedge", "1");
        params.putIfAbsent("load.use_out_degree", "1");
        params.put("load.type", "hugegraph");
        params.put("load.hg_pd_peers", json(List.of(
                this.config.get(HubbleOptions.PD_PEERS).split(","))));
        params.put("load.hugegraph_name", client.getGraphSpaceName() + "/" +
                   client.getGraphName() + "/g");
        return this.submit(client, taskType, params);
    }

    public Map<String, Object> compute(HugeClient client, Map<String, Object> options) {
        authorize(client);
        E.checkArgument(options != null && options.get("compute.algorithm") != null,
                        "compute.algorithm is required");
        String algorithm = options.get("compute.algorithm").toString();
        E.checkArgument(Set.of("pagerank", "wcc", "lpa", "degree", "closeness_centrality",
                               "betweenness_centrality", "triangle_count", "kcore", "sssp")
                           .contains(algorithm), "Unsupported Vermeer algorithm: %s", algorithm);
        Map<String, String> params = new HashMap<>();
        options.forEach((key, value) -> {
            E.checkArgument(key.startsWith("compute.") ||
                            key.startsWith(algorithm + ".") ||
                            "output.need_statistics".equals(key),
                            "Unsupported Vermeer parameter: %s", key);
            if (value != null) {
                params.put(key, value.toString());
            }
        });
        // Query results directly. Never persist a HugeGraph password or bearer
        // token in Vermeer task parameters for asynchronous output.
        params.put("output.type", "local");
        params.put("output.need_query", "1");
        params.put("output.file_path", "./data/hubble/" + UUID.randomUUID());
        return this.submit(client, "compute", params);
    }

    private Map<String, Object> submit(HugeClient client, String type,
                                       Map<String, String> params) {
        Map<String, Object> result = this.backend.request("POST", "tasks/create",
                Map.of("graph", name(client), "task_type", type, "params", params));
        Map<String, Object> task = HubbleUtil.uncheckedCast(result.get("task"));
        return Map.of("task_id", publicId(task));
    }

    public Task task(HugeClient client, int id, boolean includeResult) {
        authorize(client);
        Map<String, Object> raw = this.scopedTask(client, id);
        Task task = this.convert(raw);
        if (includeResult && task.success() && "compute".equals(raw.get("task_type"))) {
            Map<String, Object> values = this.results(client, id, 0, 1000);
            Map<String, Object> data = new HashMap<>(task.asMap());
            data.put(Task.P.RESULT, json(values));
            return this.mapper.convertValue(data, Task.class);
        }
        return task;
    }

    public Map<String, Object> results(HugeClient client, int id, int cursor, int limit) {
        authorize(client);
        this.scopedTask(client, id);
        E.checkArgument(cursor >= 0 && limit > 0 && limit <= 10000,
                        "Invalid result cursor or limit");
        Map<String, Object> result = new HashMap<>(this.backend.request(
                "GET", "tasks/value/" + nativeId(id) +
                "?cursor=" + cursor + "&limit=" + (limit + 1), null));
        List<?> vertices = HubbleUtil.uncheckedCast(result.getOrDefault("vertices", List.of()));
        int count = Math.min(vertices.size(), limit);
        // Native pages have no total or continuation flag. Peek one value ahead,
        // then leave it at the next cursor so an exact final page has no phantom successor.
        result.put("vertices", new ArrayList<>(vertices.subList(0, count)));
        result.put("cursor", cursor + count);
        result.put("has_more", vertices.size() > limit);
        return result;
    }

    public List<Task> tasks(HugeClient client) {
        if (!this.permitted(client)) {
            return List.of();
        }
        authorize(client);
        List<Map<String, Object>> items = HubbleUtil.uncheckedCast(
                this.backend.request("GET", "tasks?limit=10000", null)
                            .getOrDefault("tasks", List.of()));
        List<Task> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            if (name(client).equals(item.get("graph_name"))) {
                result.add(this.convert(item));
            }
        }
        return result;
    }

    public Task cancel(HugeClient client, int id) {
        authorize(client);
        this.scopedTask(client, id);
        this.backend.request("GET", "task/cancel/" + nativeId(id), null);
        return this.task(client, id, false);
    }

    public void deleteGraph(HugeClient client) {
        authorize(client);
        this.unloadGraph(client);
    }

    public void deleteGraphIfPresent(HugeClient client) {
        // Source deletion must not depend on interactive compute availability.
        // Keep the same authorization before using the shared service identity.
        authorize(client);
        if (this.graph(client.getGraphSpaceName(), client.getGraphName()).containsKey("graph")) {
            this.unloadGraph(client);
        }
    }

    private void unloadGraph(HugeClient client) {
        this.backend.request("DELETE", "graphs/" +
                URLEncoder.encode(name(client), StandardCharsets.UTF_8), null);
    }

    private Map<String, Object> scopedTask(HugeClient client, int id) {
        Map<String, Object> task = HubbleUtil.uncheckedCast(
                this.backend.request("GET", "task/" + nativeId(id), null).get("task"));
        if (task == null || !name(client).equals(task.get("graph_name"))) {
            throw new ForbiddenException("Task does not belong to this graph");
        }
        return task;
    }

    private Task convert(Map<String, Object> task) {
        String type = "compute".equals(task.get("task_type")) ? "compute" : "load";
        String status = task.getOrDefault("status", "created").toString();
        status = switch (status) {
            case "complete", "loaded" -> "success";
            case "error" -> "failed";
            case "canceled" -> "cancelled";
            case "created", "waiting" -> "queued";
            default -> "running";
        };
        Map<String, Object> result = new HashMap<>();
        result.put(Task.P.ID, publicId(task));
        result.put(Task.P.TYPE, "vermeer-task:" + type);
        Map<String, String> params = HubbleUtil.uncheckedCast(task.getOrDefault("params", Map.of()));
        result.put(Task.P.NAME, params.getOrDefault("compute.algorithm", "Vermeer " + type));
        result.put(Task.P.STATUS, status);
        result.put(Task.P.CREATE, time(task.get("create_time")));
        result.put(Task.P.UPDATE, time(task.get("update_time")));
        result.put(Task.P.RESULT, task.get("error_message"));
        return this.mapper.convertValue(result, Task.class);
    }

    private static long time(Object value) {
        return value == null ? 0L : OffsetDateTime.parse(value.toString()).toInstant().toEpochMilli();
    }

    private static int publicId(Map<String, Object> task) {
        int id = ((Number) task.get("id")).intValue();
        E.checkState(id > 0, "Invalid Vermeer task id");
        return -id;
    }

    private static long nativeId(int id) {
        E.checkArgument(id < 0, "Invalid direct Vermeer task id");
        return -(long) id;
    }

    private static String name(HugeClient client) {
        return name(client.getGraphSpaceName(), client.getGraphName());
    }

    private static String name(String space, String graph) {
        return space + "-" + graph;
    }

    private void authorize(HugeClient client) {
        // authClient resolves the connection and GraphSpace access. A graph
        // read additionally verifies the caller's permission before using a
        // shared Vermeer service credential or reading raw Store partitions.
        if (!this.permitted(client)) {
            throw new ForbiddenException(
                    "Direct Vermeer access requires a super administrator in authenticated mode");
        }
        client.graph().listVertices(1);
    }

    private static String json(Object value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Invalid Vermeer parameters", e);
        }
    }
}
