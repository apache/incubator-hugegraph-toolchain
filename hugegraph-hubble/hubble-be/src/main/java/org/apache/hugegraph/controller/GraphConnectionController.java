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

package org.apache.hugegraph.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.common.Response;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.GraphConnectionService;
import org.apache.hugegraph.service.HugeClientPoolService;
import org.apache.hugegraph.service.SettingSSLService;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.HubbleUtil;
import org.apache.hugegraph.util.HugeClientUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "graph-connections")
public class GraphConnectionController extends BaseController {

    private static final Pattern GRAPH_PATTERN = Pattern.compile(
            "^[A-Za-z][A-Za-z0-9_]{0,47}$"
    );

    @Autowired
    private HugeConfig config;
    @Autowired
    private GraphConnectionService connService;
    @Autowired
    private HugeClientPoolService poolService;
    @Autowired
    private SettingSSLService sslService;

    @GetMapping
    public Response list(@RequestParam(name = "content", required = false)
                         String content,
                         @RequestParam(name = "page_no", required = false,
                                       defaultValue = "1")
                         int pageNo,
                         @RequestParam(name = "page_size", required = false,
                                       defaultValue = "10")
                         int pageSize) {
        IPage<GraphConnection> conns = this.connService.list(content, pageNo,
                                                             pageSize);
        return Response.builder().status(Constant.STATUS_OK).data(conns)
                       .build();
    }

    @GetMapping("{id}")
    public GraphConnection get(@PathVariable("id") int id) {
        GraphConnection entity = this.connService.get(id);
        if (entity == null) {
            throw new ExternalException("graph-connection.not-exist.id", id);
        }
        if (!this.poolService.containsKey(id)) {
            this.sslService.configSSL(this.config, entity);
            HugeClient client = HugeClientUtil.tryConnect(entity);
            this.poolService.put(entity, client);
        }
        return entity;
    }

    @PostMapping
    public GraphConnection create(@RequestBody GraphConnection newEntity) {
        this.checkParamsValid(newEntity, true);
        this.checkAddressSecurity(newEntity);
        // Make sure the new entity doesn't conflict with exists
        this.checkEntityUnique(newEntity, true);

        newEntity.setTimeout(this.config.get(HubbleOptions.CLIENT_REQUEST_TIMEOUT));
        // Do connect test, failure will throw an exception
        this.sslService.configSSL(this.config, newEntity);
        HugeClient client = HugeClientUtil.tryConnect(newEntity);
        newEntity.setCreateTime(HubbleUtil.nowDate());

        this.connService.save(newEntity);
        this.poolService.put(newEntity, client);
        return newEntity;
    }

    @PutMapping("{id}")
    public GraphConnection update(@PathVariable("id") int id,
                                  @RequestBody GraphConnection newEntity) {
        this.checkIdSameAsBody(id, newEntity);
        this.checkParamsValid(newEntity, false);
        this.checkAddressSecurity(newEntity);

        // Check exist connection with this id
        GraphConnection oldEntity = this.connService.get(id);
        if (oldEntity == null) {
            throw new ExternalException("graph-connection.not-exist.id", id);
        }
        GraphConnection entity = this.mergeEntity(oldEntity, newEntity);
        // Make sure the updated connection doesn't conflict with exists
        this.checkEntityUnique(entity, false);
        this.sslService.configSSL(this.config, entity);
        HugeClient client = HugeClientUtil.tryConnect(entity);

        this.connService.update(entity);
        this.poolService.put(entity, client);
        return entity;
    }

    @DeleteMapping("{id}")
    public GraphConnection delete(@PathVariable("id") int id) {
        GraphConnection oldEntity = this.connService.get(id);
        if (oldEntity == null) {
            throw new ExternalException("graph-connection.not-exist.id", id);
        }
        this.connService.remove(id);
        this.poolService.remove(oldEntity);
        return oldEntity;
    }

    private void checkParamsValid(GraphConnection newEntity, boolean creating) {
        Ex.check(creating, () -> newEntity.getId() == null,
                 "common.param.must-be-null", "id");

        String name = newEntity.getName();
        this.checkParamsNotEmpty("name", name, creating);
        Ex.check(name != null, () -> Constant.COMMON_NAME_PATTERN.matcher(name)
                                                                 .matches(),
                 "graph-connection.name.unmatch-regex");

        String graph = newEntity.getGraph();
        this.checkParamsNotEmpty("graph", graph, creating);
        Ex.check(graph != null, () -> GRAPH_PATTERN.matcher(graph).matches(),
                 "graph-connection.graph.unmatch-regex");

        String host = newEntity.getHost();
        this.checkParamsNotEmpty("host", host, creating);
        Ex.check(host != null, () -> HubbleUtil.HOST_PATTERN.matcher(host)
                                                            .matches(),
                 "graph-connection.host.unmatch-regex");

        Integer port = newEntity.getPort();
        Ex.check(creating, () -> port != null,
                 "common.param.cannot-be-null", "port");
        Ex.check(port != null, () -> 0 < port && port <= 65535,
                 "graph-connection.port.must-be-in-range", "[1, 65535]", port);

        Ex.check((StringUtils.isEmpty(newEntity.getUsername()) &&
                  StringUtils.isEmpty(newEntity.getPassword())) ||
                 (!StringUtils.isEmpty(newEntity.getUsername()) &&
                  !StringUtils.isEmpty(newEntity.getPassword())),
                 "graph-connection.username-or-password.must-be-same-status");

        Ex.check(newEntity.getCreateTime() == null,
                 "common.param.must-be-null", "create_time");
    }

    private void checkAddressSecurity(GraphConnection newEntity) {
        String host = newEntity.getHost();
        Integer port = newEntity.getPort();
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new ExternalException("graph-connection.host.unresolved");
        }
        String ip = address.getHostAddress();
        log.debug("The host: {}, ip: {}", address.getHostName(), ip);

        List<String> ipWhiteList = this.config.get(
                                   HubbleOptions.CONNECTION_IP_WHITE_LIST);
        if (!ipWhiteList.contains("*")) {
            Ex.check(ipWhiteList.contains(host) || ipWhiteList.contains(ip),
                     "graph-connection.host.unauthorized");
        }

        List<Integer> portWhiteList = this.config.get(
                                      HubbleOptions.CONNECTION_PORT_WHITE_LIST);
        if (!portWhiteList.contains(-1)) {
            Ex.check(portWhiteList.contains(port),
                     "graph-connection.port.unauthorized");
        }
    }

    private void checkEntityUnique(GraphConnection newEntity,
                                   boolean creating) {
        List<GraphConnection> oldEntities = this.connService.listAll();
        for (GraphConnection oldEntity : oldEntities) {
            // NOTE: create should check all, update check others
            if (!creating && oldEntity.getId().equals(newEntity.getId())) {
                continue;
            }
            Ex.check(!oldEntity.getName().equals(newEntity.getName()),
                     "graph-connection.exist.name", oldEntity.getName());
            Ex.check(!(oldEntity.getGraph().equals(newEntity.getGraph()) &&
                     oldEntity.getHost().equals(newEntity.getHost()) &&
                     oldEntity.getPort().equals(newEntity.getPort())),
                     "graph-connection.exist.graph-host-port",
                     oldEntity.getGraph(), oldEntity.getHost(),
                     oldEntity.getPort());
        }
    }
}
