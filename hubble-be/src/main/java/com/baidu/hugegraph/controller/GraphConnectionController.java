/*
 * Copyright 2017 HugeGraph Authors
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

package com.baidu.hugegraph.controller;

import java.util.Date;
import java.util.List;

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

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.entity.GraphConnection;
import com.baidu.hugegraph.exception.ExternalException;
import com.baidu.hugegraph.exception.InternalException;
import com.baidu.hugegraph.service.GraphConnectionService;
import com.baidu.hugegraph.service.HugeClientPoolService;
import com.baidu.hugegraph.util.Ex;
import com.baidu.hugegraph.util.HugeClientUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;

@RestController
@RequestMapping("graph-connections")
public class GraphConnectionController extends BaseController {

    private static final String NAME_REGEX = "^[A-Za-z][A-Za-z0-9_]{0,47}$";
    private static final String GRAPH_REGEX = NAME_REGEX;
    private static final String HOST_REGEX =
                                "(([0-9]{1,3}\\.){3}[0-9]{1,3}" + "|" +
                                "([0-9A-Za-z_!~*'()-]+\\.)*[0-9A-Za-z_!~*'()-]+)$";

    @Autowired
    private GraphConnectionService connService;
    @Autowired
    private HugeClientPoolService poolService;

    @GetMapping
    public IPage<GraphConnection> list(@RequestParam(name = "content",
                                                     required = false)
                                       String content,
                                       @RequestParam(name = "page_no",
                                                     required = false,
                                                     defaultValue = "1")
                                       long pageNo,
                                       @RequestParam(name = "page_size",
                                                     required = false,
                                                     defaultValue = "10")
                                       long pageSize) {
        return this.connService.list(content, pageNo, pageSize);
    }

    @GetMapping("{id}")
    public GraphConnection get(@PathVariable("id") int id) {
        GraphConnection entity = this.connService.get(id);
        if (entity == null) {
            throw new ExternalException("graph-connection.not-exist.id", id);
        }
        if (!this.poolService.containsKey(id)) {
            HugeClient client = HugeClientUtil.tryConnect(entity);
            this.poolService.put(entity, client);
        }
        return entity;
    }

    @PostMapping
    public GraphConnection create(@RequestBody GraphConnection newEntity) {
        this.checkParamsValid(newEntity, true);
        // Make sure the new entity doesn't conflict with exists
        this.checkEntityUnique(newEntity, true);
        // Do connect test, failure will throw an exception
        HugeClient client = HugeClientUtil.tryConnect(newEntity);
        newEntity.setCreateTime(new Date());

        int rows = this.connService.save(newEntity);
        if (rows != 1) {
            throw new InternalException("entity.insert.failed", newEntity);
        }
        this.poolService.put(newEntity, client);
        return newEntity;
    }

    @PutMapping("{id}")
    public GraphConnection update(@PathVariable("id") int id,
                                  @RequestBody GraphConnection newEntity) {
        this.checkIdWhenUpdate(id, newEntity);
        this.checkParamsValid(newEntity, false);

        // Check exist connection with this id
        GraphConnection oldEntity = this.connService.get(id);
        if (oldEntity == null) {
            throw new ExternalException("graph-connection.not-exist.id", id);
        }
        GraphConnection entity = this.mergeEntity(oldEntity, newEntity);
        // Make sure the updated connection doesn't conflict with exists
        this.checkEntityUnique(entity, false);
        HugeClient client = HugeClientUtil.tryConnect(entity);

        int rows = this.connService.update(entity);
        if (rows != 1) {
            throw new InternalException("entity.update.failed", entity);
        }
        this.poolService.put(entity, client);
        return entity;
    }

    @DeleteMapping("{id}")
    public GraphConnection delete(@PathVariable("id") int id) {
        GraphConnection oldEntity = this.connService.get(id);
        if (oldEntity == null) {
            throw new ExternalException("graph-connection.not-exist.id", id);
        }
        int rows = this.connService.remove(id);
        if (rows != 1) {
            throw new InternalException("entity.delete.failed", oldEntity);
        }
        this.poolService.remove(oldEntity);
        return oldEntity;
    }

    private void checkParamsValid(GraphConnection newEntity, boolean creating) {
        Ex.check(creating, () -> newEntity.getId() == null,
                 "common.param.must-be-null", "id");

        String name = newEntity.getName();
        this.checkParamsNotEmpty("name", name, creating);
        Ex.check(name != null, () -> name.matches(NAME_REGEX),
                 "graph-connection.name.unmatch-regex", name);

        String graph = newEntity.getGraph();
        this.checkParamsNotEmpty("graph", graph, creating);
        Ex.check(graph != null, () -> graph.matches(GRAPH_REGEX),
                 "graph-connection.graph.unmatch-regex", graph);

        String host = newEntity.getHost();
        this.checkParamsNotEmpty("host", host, creating);
        Ex.check(host != null, () -> host.matches(HOST_REGEX),
                 "graph-connection.host.unmatch-regex", host);

        Integer port = newEntity.getPort();
        Ex.check(creating, () -> port != null,
                 "common.param.cannot-be-null", "port");
        Ex.check(port != null, () -> 0 < port && port <= 65535,
                 "graph-connection.port.must-be-in-range", "[1, 65535]", port);
        
        Ex.check(newEntity.getCreateTime() == null,
                 "common.param.must-be-null", "create_time");
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
