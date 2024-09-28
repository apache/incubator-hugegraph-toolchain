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

package org.apache.hugegraph.service.algorithm;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.algorithm.OlapEntity;
import org.apache.hugegraph.entity.query.OlapView;
import org.apache.hugegraph.service.query.ExecuteHistoryService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Log4j2
@Service
public class OlapAlgoService {
    @Autowired
    private HugeConfig config;
    @Autowired
    private ExecuteHistoryService historyService;

    public OlapView olapView(HugeClient client, String graphspace, OlapEntity body) {
        Map<String, Object> params = body.getParams();
        if (!"DEFAULT".equals(graphspace)) {
            params.put("k8s.master_cpu", "1");
            params.put("k8s.worker_cpu", "1");
            params.put("k8s.master_request_memory", "5Gi");
            params.put("k8s.worker_request_memory", "5Gi");
            params.put("k8s.master_memory", "5Gi");
            params.put("k8s.worker_memory", "5Gi");
            params.putAll(body.getParams());
        }
        long taskid = client.computer().create(body.getAlgorithm(), body.getWorker(), params);
//        String graphSpace = client.getGraphSpaceName();
//        String graph = client.getGraphName();
//        Date createTime = HubbleUtil.nowDate();
//        // Insert execute history
//        ExecuteStatus status = ExecuteStatus.SUCCESS;
//        ExecuteHistory history;
//        history = new ExecuteHistory(null, graphSpace, graph, 0L,
//                ExecuteType.ALGORITHM,
//                body.toString(), status,
//                AsyncTaskStatus.UNKNOWN, -1L, createTime);
//        this.historyService.save(history);
        return OlapView.builder().taskId(taskid).build();
    }
}
