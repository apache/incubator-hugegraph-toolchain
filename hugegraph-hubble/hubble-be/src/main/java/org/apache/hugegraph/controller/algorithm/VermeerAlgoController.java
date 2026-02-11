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

package org.apache.hugegraph.controller.algorithm;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.space.VermeerService;
import org.apache.hugegraph.util.E;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs" +
        "/{graph}/algorithms/vermeer")
public class VermeerAlgoController extends BaseController {

    @Autowired
    VermeerService vermeerService;
    @Autowired
    private HugeConfig config;

    @PostMapping
    public Map<String, Object> olapView(@PathVariable("graphspace") String graphspace,
                                        @PathVariable("graph") String graph,
                                        @RequestBody VParams body) {
        String vGraph = vermeerService.convert2VG(graphspace, graph);
        HugeClient client = this.authClient(null, null);

        Map<String, Object> graphInfo =
                (Map<String, Object>) client.vermeer()
                        .getGraphInfoByName(vGraph)
                        .get("graph");
        E.checkArgument(graphInfo != null && !graphInfo.isEmpty(),
                "graph not loaded");

        Map<String, Object> params = new HashMap<>();
        // default params
        String pdPeers = config.get(HubbleOptions.PD_PEERS);
        String pdJson = JsonUtil.toJson(Arrays.asList(pdPeers.split(",")));
        params.put("output.parallel", "10");
        params.put("output.type", "hugegraph");
        params.put("output.hg_pd_peers", pdJson);
        params.put("output.hugegraph_name", graphspace + "/" + graph + "/g");
        params.put("output.hugegraph_username", this.getUser());
        params.put("output.hugegraph_password", (String) this.getSession(
                "password"));
        params.put("output.hugegraph_property", body.params.get("compute" +
                ".algorithm"));
        // input params
        params.putAll(body.analyze());
        return vermeerService.compute(client, graphspace, graph, params);
    }

    private static class VParams {
        @JsonProperty("params")
        public Map<String, Object> params;

        public Map<String, Object> analyze() {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                entry.setValue(entry.getValue().toString());
            }
            return params;
        }
    }
}
