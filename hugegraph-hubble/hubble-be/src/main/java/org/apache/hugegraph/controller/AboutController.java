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

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.handler.MessageSourceHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
//import org.apache.hugegraph.license.LicenseVerifier; // TODO C Remove Licence


@RestController
@RequestMapping("about")
public class AboutController extends BaseController {

    @Autowired
    private MessageSourceHandler messageHandler;

    @GetMapping
    public Map<String, Object> about() {
        //LicenseVerifier verifier = LicenseVerifier.instance();// TODO C Remove Licence
        Map<String, Object> about = new LinkedHashMap<>();
        about.put("name", Constant.SERVER_NAME);
        about.put("version", "3.0.0");
        //about.put("edition", verifier.edition());// TODO C Remove Licence
        //about.put("allowed_graphs", verifier.allowedGraphs());
        //long datasize = verifier.allowedDataSize();
        //if (datasize == -1) {
        //    about.put("allowed_datasize",
        //              this.messageHandler.getMessage("license.datasize.no-limit"));
        //} else {
        //    about.put("allowed_datasize",
        //              FileUtils.byteCountToDisplaySize(datasize * Bytes.MB));
        //}
        return about;
    }
}
