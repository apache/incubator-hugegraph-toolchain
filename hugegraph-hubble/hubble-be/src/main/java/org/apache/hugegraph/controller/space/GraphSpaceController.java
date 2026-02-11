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

package org.apache.hugegraph.controller.space;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.entity.space.BuiltInEntity;
import org.apache.hugegraph.entity.space.GraphSpaceEntity;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.auth.UserService;
import org.apache.hugegraph.service.graphs.GraphsService;
import org.apache.hugegraph.service.space.GraphSpaceService;
import org.apache.hugegraph.structure.space.GraphSpace;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces")
public class GraphSpaceController extends BaseController {
    private static final int DEFAULT_MEMORY_VALUE = 128;
    private static final int DEFAULT_CPU_VALUE = 64;

    @Autowired
    private GraphSpaceService graphSpaceService;

    @Autowired
    private UserService userService;

    @Autowired
    GraphsService graphsService;
    @Autowired
    HugeConfig config;

    @GetMapping("list")
    public Object list() {

        List<String> graphSpaces =
                this.graphSpaceService.listAll(this.authClient(null, null));
        return ImmutableMap.of("graphspaces", graphSpaces);
    }

    @GetMapping
    public Object queryPage(@RequestParam(name = "query", required = false,
                                    defaultValue = "") String query,
                            @RequestParam(name = "create_time", required = false,
                                    defaultValue = "") String createTime,
                            @RequestParam(name = "page_no", required = false,
                                    defaultValue = "1") int pageNo,
                            @RequestParam(name = "page_size", required = false,
                                    defaultValue = "10") int pageSize,
                            @RequestParam(name = "all", required = false,
                                          defaultValue = "false") boolean all) {
        if (all) {
            return this.graphSpaceService.queryAllGs(
                    this.authClient(null, null), query, createTime);
        }
        return graphSpaceService.queryPage(this.authClient(null, null),
                                           query, createTime, pageNo, pageSize);
    }

    @GetMapping("{graphspace}/auth")
    public Object isAuth(@PathVariable("graphspace") String graphSpace) {
        boolean isAuth = graphSpaceService.isAuth(this.authClient(null, null),
                                                  graphSpace);
        return ImmutableMap.of("auth", isAuth);
    }

    @GetMapping("{graphspace}")
    public GraphSpaceEntity get(@PathVariable("graphspace") String graphspace) {
        HugeClient client = this.authClient(null, null);
        // Get GraphSpace Info
        return graphSpaceService.getWithAdmins(client, graphspace);
    }

    @PostMapping
    public Object add(@RequestBody GraphSpaceEntity graphSpaceEntity) {
        // Create GraphSpace
        HugeClient client = this.authClient(null, null);
        if (graphSpaceEntity.getCpuLimit() <= 0) {
            graphSpaceEntity.setCpuLimit(DEFAULT_CPU_VALUE);
        }
        if (graphSpaceEntity.getMemoryLimit() <= 0) {
            graphSpaceEntity.setMemoryLimit(DEFAULT_MEMORY_VALUE);
        }
        if (graphSpaceEntity.getComputeCpuLimit() <= 0) {
            graphSpaceEntity.setComputeCpuLimit(DEFAULT_CPU_VALUE);
        }
        if (graphSpaceEntity.getComputeMemoryLimit() <= 0) {
            graphSpaceEntity.setComputeCpuLimit(DEFAULT_MEMORY_VALUE);
        }

        graphSpaceService.create(client, graphSpaceEntity.convertGraphSpace());

        // Add GraphSpace Admin
        graphSpaceEntity.graphspaceAdmin.forEach(u -> {
            client.auth().addSpaceAdmin(u, graphSpaceEntity.getName());
        });

        return get(graphSpaceEntity.getName());
    }

    @PutMapping("{graphspace}")
    public GraphSpace update(@PathVariable("graphspace") String graphspace,
                             @RequestBody GraphSpaceEntity graphSpaceEntity) {

        graphSpaceEntity.setName(graphspace);

        HugeClient client = this.authClient(null, null);

        // Update graphspace
        graphSpaceService.update(client, graphSpaceEntity.convertGraphSpace());

        // Update graphspace admin
        ImmutableSet<String> oldSpaceAdmins
                = ImmutableSet.copyOf(userService.listGraphSpaceAdmin(client,
                                                                      graphspace));
        ImmutableSet<String> curSpaceAdmins
                = ImmutableSet.copyOf(graphSpaceEntity.graphspaceAdmin);

        // a. Del
        SetUtils.difference(oldSpaceAdmins, curSpaceAdmins).forEach(u -> {
            client.auth().delSpaceAdmin(u, graphspace);
        });
        // b. Add
        SetUtils.difference(curSpaceAdmins, oldSpaceAdmins).forEach(u -> {
            client.auth().addSpaceAdmin(u, graphspace);
        });

        return get(graphSpaceEntity.getName());
    }

    @DeleteMapping("{graphspace}")
    public void delete(@PathVariable("graphspace") String graphspace) {
        E.checkArgument(StringUtils.isNotEmpty(graphspace), "graphspace " +
                "must not null");

        HugeClient client = this.authClient(null, null);

        // Delete graphspace admin
        userService.listGraphSpaceAdmin(client, graphspace).forEach(u -> {
            client.auth().delSpaceAdmin(u, graphspace);
        });

        // delete graphspace
        graphSpaceService.delete(client, graphspace);
    }

    @GetMapping("{graphspace}/setdefault")
    public Map<String, String> setdefault(@PathVariable("graphspace") String graphspace) {
        return graphSpaceService.setdefault(this.authClient(null, null), graphspace);
    }

    @GetMapping("getdefault")
    public Map<String, String> getdefault() {
        return graphSpaceService.getdefault(this.authClient(null, null));
    }

    @PostMapping("builtin")
    public Object initBuiltIn(@RequestBody BuiltInEntity entity) {
        GraphConnection connection = new GraphConnection();

        String url = this.getUrl();
        UrlUtil.Host host = UrlUtil.parseHost(url);
        connection.setProtocol(host.getScheme());
        connection.setHost(host.getHost());
        connection.setPort(host.getPort());

        connection.setCluster(config.get(HubbleOptions.PD_CLUSTER));
        connection.setRouteType(config.get(HubbleOptions.ROUTE_TYPE));
        connection.setPdPeers(config.get(HubbleOptions.PD_PEERS));
        connection.setGraphSpace(Constant.BUILT_IN);
        connection.setToken(this.getToken());

        HugeClient client = this.authClient(null, null);
        Ex.check(userService.isSuperAdmin(client), "仅限系统管理员操作");
        if (entity.initSpace) {
            graphSpaceService.initBuiltIn(client);
        }
        client.assignGraph(Constant.BUILT_IN, null);
        graphsService.initBuiltIn(client, connection, entity);
        return null;
    }
}
