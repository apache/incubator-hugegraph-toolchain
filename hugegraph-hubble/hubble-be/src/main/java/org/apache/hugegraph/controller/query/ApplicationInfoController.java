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

package org.apache.hugegraph.controller.query;

import org.apache.hugegraph.common.AppType;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.controller.saas.SaaSMetricsController;
import org.apache.hugegraph.entity.query.ApplicationInfo;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.query.ApplicationInfoService;
import org.apache.hugegraph.util.E;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Constant.API_VERSION +
                "graphspaces/{graphspace}/graphs/{graph}/applications")
public class ApplicationInfoController extends BaseController {
    @Autowired
    private ApplicationInfoService appInfoService;

    @Autowired
    private HugeConfig config;


    @PostMapping
    public ApplicationInfo insertOrUpdateAppInfo(
            @PathVariable("graphspace") String graphSpace,
            @PathVariable("graph") String graph,
            @RequestBody ApplicationInfo appInfo) {
        appInfo.setGraphName(join(config.get(HubbleOptions.IDC), graphSpace, graph));
        checkParams(appInfo, graphSpace);
        appInfoService.insertOrUpdateAppInfo(appInfo);
        return appInfo;
    }

    @GetMapping
    public List<ApplicationInfo> list(
            @PathVariable("graphspace") String graphSpace,
            @PathVariable("graph") String graph) {
        String idc = config.get(HubbleOptions.IDC);
        //query apps from database
        List<ApplicationInfo> apps =
                appInfoService.query(join(idc, graphSpace, graph));

        // add three default apps
        apps.add(SaaSMetricsController.defaultGremlinApp(idc, graphSpace, graph));
        apps.add(SaaSMetricsController.defaultVertexUpdateApp(idc, graphSpace, graph));
        apps.add(SaaSMetricsController.defaultEdgeUpdateApp(idc, graphSpace, graph));
        return apps;
    }

    @GetMapping("/{appName}/{appType}")
    public ApplicationInfo appInfo(
            @PathVariable("graphspace") String graphSpace,
            @PathVariable("graph") String graph,
            @PathVariable("appName") String appName,
            @PathVariable("appType") String appType) {
        String idc = config.get(HubbleOptions.IDC);
        if (AppType.GENERAL.name().equals(appType) && "GREMLIN".equals(appName)) {
            return SaaSMetricsController.defaultGremlinApp(idc, graphSpace, graph);
        }
        if (AppType.GENERAL.name().equals(appType) && "VERTEX-UPDATE".equals(appName)) {
            return SaaSMetricsController.defaultVertexUpdateApp(idc, graphSpace, graph);
        }
        if (AppType.GENERAL.name().equals(appType) && "EDGE-UPDATE".equals(appName)) {
            return SaaSMetricsController.defaultEdgeUpdateApp(idc, graphSpace, graph);
        }

        List<ApplicationInfo> apps =
                appInfoService.query(join(idc, graphSpace, graph), appName, appType);
        assert apps != null && apps.size() == 1;
        return apps.get(0);
    }

    @DeleteMapping("/{appName}/{appType}")
    public void delete(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("graph") String graph,
                       @PathVariable("appName") String appName,
                       @PathVariable("appType") String appType) {
        appInfoService.delete(
                join(config.get(HubbleOptions.IDC), graphSpace, graph), appName, appType);
    }

    private void checkParams(ApplicationInfo appInfo, String graphspace) {
        E.checkArgument(
                appInfo.getGraphName() != null &&
                appInfo.getAppType() != null &&
                appInfo.getAppName() != null &&
                appInfo.getCountQuery() != null &&
                appInfo.getDistributionQuery() != null,
                "application info each filed can not be null!");
    }

    private static String join(String idc, String graphSpace, String graph) {
        return String.join("-", idc, graphSpace, graph);
    }
}
