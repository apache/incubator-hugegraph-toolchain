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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.rest.SerializeException;
import org.apache.hugegraph.util.JsonUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.space.ComputerServiceEntity;
import org.apache.hugegraph.structure.Task;
import org.apache.hugegraph.util.PageUtil;

@Log4j2
@Service
public class ComputerService {
    public IPage<ComputerServiceEntity> queryPage(HugeClient client,
                                                  String query,
                                                  int pageNo,
                                                  int pageSize) {
        ArrayList results = new ArrayList<ComputerService>();
        List<Task> tasks = client.computer().list(500);
        tasks.stream().skip(Math.max(pageNo - 1, 0) * pageSize).limit(pageSize)
             .forEach((t) -> {
                 ComputerServiceEntity entity = get(client, t.id());
                 results.add(entity);
             });

        int count = tasks.size();

        return PageUtil.newPage(results, pageNo, pageSize, count);
    }

    public ComputerServiceEntity get(HugeClient client, long id) {
        return convert(client.computer().get(id));
    }

    public void cancel(HugeClient client, long id) {
        client.computer().cancel(id);
    }

    protected ComputerServiceEntity convert(Task task) {
        ComputerServiceEntity entity = new ComputerServiceEntity();
        entity.setId(task.id());
        entity.setName(task.name());
        entity.setType(task.type());
        entity.setStatus(task.status());
        entity.setProgress(task.progress());
        entity.setDescription(task.description());
        entity.setCreate(task.createTime());

        if (StringUtils.isNotEmpty(task.input())) {
            try {
                Map<String, Object> input = JsonUtil.fromJson(task.input(),
                                                              Map.class);

                entity.setAlgorithm(input.get("algorithm").toString());
                entity.setInput(input.get("params").toString());
            } catch (SerializeException e) {
                log.info("load task.input error", e);
            }
        }
        return entity;
    }

    public void delete(HugeClient client, long id) {
        client.computer().delete(id);
    }
}
