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

import org.apache.hugegraph.util.E;
import org.apache.hugegraph.exception.ParameterizedException;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.service.space.OLTPServerService;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.structure.space.OLTPService;

import java.util.List;

@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/services/oltp")
public class ServiceController extends BaseController {

    @Autowired
    OLTPServerService oltpService;

    @GetMapping
    public Object queryPage(@PathVariable("graphspace") String graphspace,
                            @RequestParam(name = "query", required = false,
                                    defaultValue = "") String query,
                            @RequestParam(name = "page_no", required = false,
                                    defaultValue = "1") int pageNo,
                            @RequestParam(name = "page_size", required = false,
                                    defaultValue = "10") int pageSize) {
        try (HugeClient client = defaultClient(graphspace, null);){
            return oltpService.queryPage(client, query, pageNo, pageSize);
        } catch (Throwable t) {
            throw t;
        }
    }

    @GetMapping("{service}")
    public Object get(@PathVariable("graphspace") String graphspace,
                      @PathVariable("service") String service) {
        try (HugeClient client = defaultClient(graphspace, null);){
            return oltpService.get(client, service);
        } catch (Throwable t) {
            throw t;
        }
    }

    @PostMapping
    public Object create(@PathVariable("graphspace") String graphspace,
                         @RequestBody OLTPService serviceEntity) {

        // return serviceEntity;
        // TODO url or routetype
        if (serviceEntity.getDepleymentType()
                == OLTPService.DepleymentType.MANUAL) {
            serviceEntity.setRouteType(null);
        } else {
            serviceEntity.setRouteType("NodePort");
            serviceEntity.setUrls(null);
        }

        try (HugeClient client = defaultClient(graphspace, null);){
            return oltpService.create(client, serviceEntity);
        } catch (Throwable t) {
            throw t;
        }
    }

    @PutMapping("{service}")
    public Object update(@PathVariable("graphspace") String graphspace,
                         @PathVariable("service") String service,
                         @RequestBody OLTPService serviceEntity) {

        E.checkArgument(!serviceEntity.getDepleymentType()
                                      .equals(OLTPService.DepleymentType.MANUAL),
                        "service.manual.disable.modify");

        serviceEntity.setName(service);
        serviceEntity.setRouteType("NodePort");
        serviceEntity.setUrls(null);

        try (HugeClient client = defaultClient(graphspace, null);){
            return oltpService.update(client, serviceEntity);
        } catch (Throwable t) {
            throw t;
        }
    }

    @DeleteMapping("{service}")
    public void delete(@PathVariable("graphspace") String graphspace,
                       @PathVariable("service") String service) {
        if (("DEFAULT".equals(graphspace)) && ("DEFAULT".equals(service))) {
            throw new ParameterizedException("Do not delete the service " +
                                             "'DEFAULT' under the graphspace " +
                                             "named 'DEFAULT'!");
        }
        try (HugeClient client = defaultClient(graphspace, null);){
            oltpService.delete(client, service);
        } catch (Throwable t) {
            throw t;
        }
    }

    @GetMapping("{service}/start")
    public void start(@PathVariable("graphspace") String graphspace,
                       @PathVariable("service") String service) {

        try (HugeClient client = defaultClient(graphspace, null);){
            oltpService.start(client, service);
        } catch (Throwable t) {
            throw t;
        }
    }

    @GetMapping("{service}/stop")
    public void stop(@PathVariable("graphspace") String graphspace,
                      @PathVariable("service") String service) {

        try (HugeClient client = defaultClient(graphspace, null);){
            oltpService.stop(client, service);
        } catch (Throwable t) {
            throw t;
        }
    }

    @GetMapping("options/list")
    public Object configOptions(@PathVariable("graphspace") String graphspace) {
        try (HugeClient client = defaultClient(graphspace, null);){
            List<String> fields = oltpService.configOptionList(client);

            return ImmutableMap.of("options", fields);
        } catch (Throwable t) {
            throw t;
        }
    }

}
