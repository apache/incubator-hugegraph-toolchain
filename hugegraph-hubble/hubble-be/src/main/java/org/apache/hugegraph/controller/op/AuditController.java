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

//package org.apache.hugegraph.controller.op;
// TODO RECOVER WHEN EVALUATED
//import lombok.SneakyThrows;
//import org.apache.hugegraph.common.Constant;
//import org.apache.hugegraph.controller.BaseController;
//import org.apache.hugegraph.entity.op.AuditEntity;
//import org.apache.hugegraph.exception.InternalException;
//import org.apache.hugegraph.logger.AuditOperationEnum;
//import org.apache.hugegraph.service.op.AuditService;
//import org.apache.hugegraph.util.JsonUtil;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.nio.charset.StandardCharsets;
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping(Constant.API_VERSION + "audits")
//public class AuditController extends BaseController {
//    @Autowired
//    AuditService auditService;
//
//    @GetMapping("operations/list")
//    public Object listOperations() {
//        List<String> operations = Arrays.stream(AuditOperationEnum.values())
//                                        .map(AuditOperationEnum::getName)
//                                        .collect(Collectors.toList());
//
//        return operations;
//    }
//
//    @GetMapping("services/list")
//    public Object listServices() throws IOException {
//        List<String> operations = auditService.listServices();
//
//        return operations;
//    }
//
//    @SneakyThrows
//    @PostMapping("query")
//    public Object query(@RequestBody AuditService.AuditReq auditReq) {
//        return auditService.queryPage(auditReq);
//    }
//
//    @PostMapping("export")
//    public void export(HttpServletResponse response,
//                         @RequestBody AuditService.AuditReq auditReq) {
//        String fileName = String.format("audit.txt", auditReq.startDatetime,
//                                        auditReq.endDatetime);
//
//        response.setCharacterEncoding("UTF-8");
//        response.setContentType("application/octet-stream");
//        response.setHeader("Access-Control-Expose-Headers",
//                           "Content-Disposition");
//        response.setHeader("Content-Disposition",
//                           "attachment;filename=" + fileName);
//        try {
//            OutputStream os = response.getOutputStream();
//            for (AuditEntity auditEntity : auditService.export(auditReq)) {
//                os.write((JsonUtil.toJson(auditEntity) + "\n")
//                                 .getBytes(StandardCharsets.UTF_8));
//            }
//            os.close();
//        } catch (IOException e) {
//            throw new InternalException("Audit Log Write Error", e);
//        }
//    }
//}
