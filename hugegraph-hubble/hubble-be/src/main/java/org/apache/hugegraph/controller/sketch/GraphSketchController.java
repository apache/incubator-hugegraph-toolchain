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

package org.apache.hugegraph.controller.sketch;///*
// * Copyright 2017 HugeGraph Authors
// *
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements. See the NOTICE file distributed with this
// * work for additional information regarding copyright ownership. The ASF
// * licenses this file to You under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// * License for the specific language governing permissions and limitations
// * under the License.
// */
//
//package org.apache.hugegraph.controller.sketch;
//
//import org.apache.hadoop.fs.FSDataInputStream;
//import org.apache.hadoop.fs.FileStatus;
//import org.apache.hadoop.fs.FileSystem;
//import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.fs.PathFilter;
//import org.apache.hugegraph.config.HugeConfig;
//import org.apache.hugegraph.util.E;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.apache.hadoop.conf.Configuration;
//
//import java.net.URI;
//import java.net.URLEncoder;
//import java.util.List;
//import java.util.Map;
//
//import javax.servlet.ServletOutputStream;
//import javax.servlet.http.HttpServletResponse;
//
//import org.apache.hugegraph.client.api.graph.GraphSketchAPI;
//import org.apache.hugegraph.common.Constant;
//import org.apache.hugegraph.controller.BaseController;
//import org.apache.hugegraph.driver.HugeClient;
//import org.apache.hugegraph.options.HubbleOptions;
//
//
//@RestController
//@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs/" +
//                "{graph}/graph-sketch")
//public class GraphSketchController extends BaseController {
//
//    @Autowired
//    private HugeConfig config;
//
//    @GetMapping
//    public Object get(@PathVariable("graphspace") String graphSpace,
//                     @PathVariable("graph") String graph) {
//        HugeClient hugeClient = this.authClient(graphSpace, graph);
//        return hugeClient.graph().getSketch();
//    }
//
//    @PostMapping
//    public Object createSketchTask(
//            @PathVariable("graphspace") String graphSpace,
//            @PathVariable("graph") String graph,
//            @RequestBody Map<String, List<String>> algorithms) {
//        List<String> list = algorithms.get("algorithms");
//        E.checkArgument(algorithms.get("algorithms") != null &&
//                        algorithms.size() == 1, "Invalid algorithms param.");
//        HugeClient hugeClient = this.authClient(graphSpace, graph);
//        GraphSketchAPI.JsonSketchTask sketchTask = GraphSketchAPI.JsonSketchTask
//                .builder().algorithms(list)
//                .afsPassword(config.get(HubbleOptions.AFS_PASSWORD))
//                .afsUser(config.get(HubbleOptions.AFS_USER))
//                .afsUri(config.get(HubbleOptions.AFS_URI))
//                .afsDir(config.get(HubbleOptions.AFS_DIR))
//                .username(this.getUser())
//                .password((String) this.getSession("password"))
//                .build();
//        return hugeClient.graph().createSketchTask(sketchTask);
//    }
//
//
//    @GetMapping("/download")
//    public void download(
//            @PathVariable("graphspace") String graphSpace,
//            @PathVariable("graph") String graph,
//            @RequestParam(name = "afs_uri", required = true) String afsUri,
//            @RequestParam(name = "afs_file", required = true) String afsFile,
//            HttpServletResponse response) throws Exception {
//        // 获取配置
//        String afsUriConf = config.get(HubbleOptions.AFS_URI);
//        String afsDirConf = config.get(HubbleOptions.AFS_DIR);
//        String afsUserConf = config.get(HubbleOptions.AFS_USER);
//        String afsPasswordConf = config.get(HubbleOptions.AFS_PASSWORD);
//
//        // 校验参数
//        E.checkArgument(afsUriConf.equals(afsUri), "Invalid afs uri");
//        E.checkArgument(afsFile.startsWith(afsDirConf), "Invalid afs file");
//        String fileName = afsFile.substring(afsDirConf.length());
//        String[] split = fileName.split("-");
//        E.checkArgument(split[0].equals(graphSpace), "Invalid graphspace");
//        E.checkArgument(split[1].equals(graph), "Invalid graph");
//
//        // 配置afs conf
//        Configuration conf = new Configuration();
//        conf.set("fs.afs.impl", "org.apache.hadoop.fs.DFileSystem");
//        conf.set("fs.AbstractFileSystem.afs.impl", "org.apache.hadoop.fs.Afs");
//        conf.set("hadoop.job.ugi", afsUserConf + "," + afsPasswordConf);
//        conf.set("dfs.use.native.api", "0"); // 0是默认值，需要afs_agent，1不需要afs_agent
//        URI uri = new URI(afsUriConf);
//        FileSystem fs = FileSystem.get(uri, conf);
//
//        // 获取文件列表
//        Path directoryPath = new Path(afsDirConf);
//        // 获取fileName开头的所有文件 (vermeer写入的文件数目不固定)
//        FileStatus[] fileStatuses =
//                fs.listStatus(directoryPath, new PathFilter() {
//                    @Override
//                    public boolean accept(Path path) {
//                        return path.getName().startsWith(fileName);
//                    }
//                });
//        response.setHeader("content-disposition", "attachment;filename=" +
//                                                  URLEncoder.encode(fileName, "utf-8"));
//        ServletOutputStream outputStream = response.getOutputStream();
//        // 遍历所有文件，将文件内容写入输出流
//        for (FileStatus status : fileStatuses) {
//            Path filePath = status.getPath();
//            FSDataInputStream inputStream = fs.open(filePath);
//            // 读取文件并写入输出流
//            byte[] bytes = new byte[1024 * 10];
//            int read;
//            do {
//                read = inputStream.read(bytes);
//                outputStream.write(bytes, 0, read);
//            } while (-1 != read);
//            inputStream.close();
//        }
//        outputStream.close();
//    }
//}
//TODO removed
