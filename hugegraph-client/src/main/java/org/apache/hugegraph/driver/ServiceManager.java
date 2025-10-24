/*
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

package org.apache.hugegraph.driver;

import java.util.List;
import java.util.Map;

import org.apache.hugegraph.api.space.ConfigAPI;
import org.apache.hugegraph.api.space.ServiceAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.structure.space.OLTPService;

public class ServiceManager {

    private final ServiceAPI serviceAPI;
    private final ConfigAPI configAPI;

    public ServiceManager(RestClient client, String graphSpace) {
        this.serviceAPI = new ServiceAPI(client, graphSpace);
        this.configAPI = new ConfigAPI(client, graphSpace);
    }

    public List<String> listService() {
        return serviceAPI.list();
    }

    public OLTPService getService(String name) {
        OLTPService service = this.serviceAPI.get(name);

        Map<String, Object> config = this.configAPI.get(name);
        service.setConfigs(config);

        return service;
    }

    public OLTPService addService(OLTPService service) {
        // Add service config info
        this.configAPI.add(service.getName(), service.getConfigs());

        // Start service
        OLTPService.OLTPServiceReq req
                = OLTPService.OLTPServiceReq.fromBase(service);
        this.serviceAPI.add(req);

        return getService(service.getName());
    }

    public void delService(String name, String message) {
        this.configAPI.delete(name);
        this.serviceAPI.delete(name, message);
    }

    public OLTPService updateService(OLTPService service) {
        if (service.checkIsK8s()) {
            // Only update config
            this.configAPI.update(service.getName(), service.getConfigs());
            // Delete and recreate service
            this.delService(service.getName(),
                            "I'm sure to delete the service");
            return addService(service);
        } else {
            // Manually created service
            this.delService(service.getName(),
                            "I'm sure to delete the service");
            return addService(service);
        }
    }

    public void startService(String name) {
        this.serviceAPI.startService(name);
    }

    public void stopService(String name) {
        this.serviceAPI.stopService(name);
    }

    /**
     * Check if service modification requires k8s pod restart by comparing
     *
     * @param service
     * @return
     */
    public boolean checkIfReloadK8sService(OLTPService service) {
        OLTPService curService = getService(service.getName());
        return service.getCount() != curService.getCount() ||
               service.getCpuLimit() != curService.getCpuLimit() ||
               service.getMemoryLimit() != curService.getMemoryLimit();
    }

    public List<String> configOptinList() {
        return this.configAPI.listConfigOptions();
    }
}
