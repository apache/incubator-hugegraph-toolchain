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

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.WhiteIpListManager;
import org.apache.hugegraph.entity.auth.WhiteIpListEntity;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
public class WhiteIpListService extends AuthService{
    public Map<String, Object> get(HugeClient client) {
        WhiteIpListManager whiteIpListManager = client.whiteIpListManager();
        Map<String, Object> whiteIpList = whiteIpListManager.list();
        return whiteIpList;
    }

    public Map<String, Object> batch(HugeClient client, WhiteIpListEntity whiteIpListEntity) {
        WhiteIpListManager whiteIpListManager = client.whiteIpListManager();
        Map<String, Object> actionMap = new HashMap<>();
        actionMap.put("action", whiteIpListEntity.getAction());
        actionMap.put("ips", whiteIpListEntity.getIps());
        Map<String, Object> whiteIpList = whiteIpListManager.batch(actionMap);
        return whiteIpList;
    }

    public Map<String, Object> updatestatus(HugeClient client, boolean status) {
        WhiteIpListManager whiteIpListManager = client.whiteIpListManager();
        Map<String, Object> whiteIpList = whiteIpListManager.update(status);
        return whiteIpList;
    }
}
