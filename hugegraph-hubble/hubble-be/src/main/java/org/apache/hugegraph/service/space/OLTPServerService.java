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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.hugegraph.driver.factory.PDHugeClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.util.PageUtil;
import org.apache.hugegraph.structure.space.OLTPService;
import org.springframework.util.CollectionUtils;

@Service
public class OLTPServerService {

    @Autowired
    protected String cluster;
    @Autowired
    PDHugeClientFactory pdHugeClientFactory;

    public IPage<Object> queryPage(HugeClient client, String query,
                                        int pageNo, int pageSize) {
        List<String> serviceNames = client.serviceManager().listService();
        List<Object> result
                = serviceNames.stream().filter(s -> s.contains(query)).sorted()
                              .map((s) -> get(client, s))
                              .collect(Collectors.toList());

        return PageUtil.page(result, pageNo, pageSize);
    }

    public Object get(HugeClient client, String serviceName) {
        // 获取当前所属图空间
        String graphSpace = client.getGraphSpaceName();
        OLTPService service = client.serviceManager().getService(serviceName);

        // 通过PD， 获取当前service可用URL, 判断当前服务是否存活
        List<String> urls = pdHugeClientFactory.getURLs(cluster, graphSpace,
                                                        serviceName);
        urls = urls.stream().distinct().collect(Collectors.toList());

        // service使用pd中的urls
        service.setUrls(urls);

        if (!service.checkIsK8s()) {
            // manual service： 通过PD判断当前服务状态
            // 设置当前运行节点数
            service.setRunning((int) urls.size());

            if (!CollectionUtils.isEmpty(urls)) {
                service.setStatus(OLTPService.ServiceStatus.RUNNING);
            } else {
                service.setStatus(OLTPService.ServiceStatus.STOPPED);
            }
        }

        return service;
    }

    public Object create(HugeClient client, OLTPService service) {
        return client.serviceManager().addService(service);
    }

    public void delete(HugeClient client, String service) {
        client.serviceManager().delService(service, "I'm sure to delete the service");
    }

    public Object update(HugeClient client, OLTPService service) {
        return client.serviceManager().updateService(service);
    }

    public void start(HugeClient client, String service) {
        client.serviceManager().startService(service);
    }

    public void stop(HugeClient client, String service) {
        client.serviceManager().stopService(service);
    }

    public List<String> configOptionList(HugeClient client) {
        return client.serviceManager().configOptinList();
    }
}
