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

package org.apache.hugegraph.service.auth;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.util.PageUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;
import lombok.extern.log4j.Log4j2;

import org.apache.hugegraph.structure.auth.Target;
import org.apache.hugegraph.exception.ExternalException;

@Log4j2
@Service
public class TargetService extends AuthService{


    public List<Target> list(HugeClient client) {
        List<Target> targets = client.auth().listTargets();

        return targets;
    }

    public IPage<Target> queryPage(HugeClient client, String query, int pageNo,
                                   int pageSize) {

        List<Target> results =
                list(client).stream()
                            .filter(target -> target.name().toLowerCase().contains(query.toLowerCase()))
                            .sorted(Comparator.comparing(Target::name))
                            .collect(Collectors.toList());

        return PageUtil.page(results, pageNo, pageSize);
    }

    public Target get(HugeClient client, String tid) {
        Target target = client.auth().getTarget(tid);
        if (target == null) {
            throw new ExternalException("auth.target.not-exist.id", tid);
        }
        return target;
    }

    public Target add(HugeClient client, Target target) {
        return client.auth().createTarget(target);
    }

    public Target update(HugeClient client, Target target) {
        return client.auth().updateTarget(target);
    }

    public void delete(HugeClient client, String tid) {
        client.auth().deleteTarget(tid);
    }
}
