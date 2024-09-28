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

package org.apache.hugegraph.controller.op;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import org.apache.hugegraph.service.space.HStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.structure.space.HStoreNodeInfo;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.exception.ExternalException;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "services/storage")
public class HStoreController extends BaseController {

    @Autowired
    HStoreService hStoreService;

    @GetMapping
    public Object listPage(@RequestParam(name = "page_no", required = false,
                                   defaultValue = "1") int pageNo,
                           @RequestParam(name = "page_size", required = false,
                                   defaultValue = "10") int pageSize) {

        HugeClient client = this.authClient(null, null);
        return hStoreService.listPage(client, pageNo, pageSize);
    }

    @GetMapping("nodes/{id}")
    public Object get(@PathVariable("id") String id) {
        HugeClient client = this.authClient(null, null);
        E.checkNotNull(id, "id");

        List<HStoreNodeInfo.HStorePartitionInfo> partitions =
                client.hStoreManager().get(id).hStorePartitionInfoList();
        return ImmutableMap.of("shards", partitions);
    }

    /**
     *
     * @return the status of sotre cluster
     * 	"Cluster_OK": "正常",
     * 	"Batch_import_Mode": "单副本入库模式"
     * 	"Partition_Split": "数据分裂中"
     * 	"Cluster_Not_Ready": "集群未就绪"
     * 	"Cluster_Fault": "集群异常"
     */
    @GetMapping("status")
    public Object status() {
        HugeClient client = this.authClient(null, null);
        String status = client.hStoreManager().status();

        return ImmutableMap.of("status", status);
    }

    /**
     * Trigger the store cluster to start splitting
     */
    @GetMapping("split")
    public void triggerSplit() {
        HugeClient client = this.authClient(null, null);
        client.hStoreManager().startSplit();
    }

    /**
     * startup list of servers to be respecified by params
     * @param request {"nodes": ["node_id1", ]}
     */
    @PostMapping("nodes/startup")
    public void nodesStartup(@RequestBody Map<String, List<String>> request) {
        List<String> nodes = request.getOrDefault("nodes", ImmutableList.of());

        E.checkNotEmpty(nodes, "nodes");

        HugeClient client = this.authClient(null, null);

        List<String> successNodes = new ArrayList<>();

        for(String nodeId: nodes) {
            try {
                client.hStoreManager().nodeStartup(nodeId);
            } catch (RuntimeException e) {
                log.info("startup hstore node({}) success", successNodes);
                log.warn("startup hstore node({}) error", nodeId, e);
                String msg = String.format("shutdown(%s) error: %s", nodeId,
                                           e.getMessage());
                throw new ExternalException(msg, e);
            }

            successNodes.add(nodeId);
        }
    }

    /**
     * startup list of servers to be respecified by params
     * @param request
     */
    @PostMapping("nodes/shutdown")
    public void nodesShutdown(@RequestBody Map<String, List<String>> request) {
        List<String> nodes = request.getOrDefault("nodes", ImmutableList.of());

        E.checkNotEmpty(nodes, "nodes");

        HugeClient client = this.authClient(null, null);

        List<String> successNodes = new ArrayList<>();

        for(String nodeId: nodes) {
            try {
                client.hStoreManager().nodeShutdown(nodeId);
            } catch (RuntimeException e) {
                log.info("shutdown hstore node({}) success", successNodes);
                log.warn("shutdown hstore node({}) error", nodeId, e);
                String msg = String.format("shutdown(%s) error: %s", nodeId,
                                           e.getMessage());
                throw new ExternalException(msg, e);
            }

            successNodes.add(nodeId);
        }
    }
}
