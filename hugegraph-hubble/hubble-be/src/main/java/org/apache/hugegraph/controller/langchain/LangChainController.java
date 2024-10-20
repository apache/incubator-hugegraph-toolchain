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

package org.apache.hugegraph.controller.langchain;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.HashMap;

import org.apache.hugegraph.controller.query.GremlinController;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.entity.query.GremlinQuery;
import org.apache.hugegraph.entity.query.JsonView;
import org.apache.hugegraph.service.query.QueryService;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.JsonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.util.E;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * langchain controller
 */
@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs/{graph}")
public class LangChainController extends BaseController {

    private static final String DEFAULT_PYTHON_FILE = "langchaincode/excute_langchain.py";

    private static final String G_V = "g.v";
    private static final String G_E = "g.e";

    private static final String WENXIN_4_MODEL = "wenxin4";
    private static final String GPT_4_MODEL = "gpt4";

    private static final List<String> DEFAULT_MODEL = Arrays.asList(WENXIN_4_MODEL, GPT_4_MODEL);

    @Autowired
    private QueryService queryService;

    @PostMapping("langchain")
    public Object langchain(@PathVariable("graphspace") String graphSpace,
                            @PathVariable("graph") String graph,
                            @RequestBody RequestLangChainParams requestLangChainParams) {
        E.checkNotNull(requestLangChainParams, "params must not be null");
        log.info("LangChainController langchain params:{}");
        this.checkParams(requestLangChainParams);
        this.checkModelParams(requestLangChainParams);
        this.checkUserParam(requestLangChainParams);

        this.tryLogin(graphSpace, graph,
                requestLangChainParams.userName, requestLangChainParams.password);

        return this.langChainQuery(graphSpace, graph, requestLangChainParams);
    }

    @PostMapping("langchain/hubble")
    public Object langchainHubble(@PathVariable("graphspace") String graphSpace,
                                  @PathVariable("graph") String graph,
                                  @RequestBody RequestLangChainParams requestLangChainParams) {
        E.checkNotNull(requestLangChainParams, "params must not be null");
        log.info("LangChainController langchain params:{}",
                 requestLangChainParams);
        this.checkParams(requestLangChainParams);

        return this.langChainQuery(graphSpace, graph, requestLangChainParams);
    }


    private ResponseLangChain langChainQuery(String graphSpace, String graph,
                                             RequestLangChainParams requestLangChainParams) {
        //uuapLoginController.tryLogin(graphSpace, graph, "admin", "S3#rd6(sg!"); //TODO C Deleted
        HugeClient client = this.authClient(graphSpace, graph);
        SchemaManager schemaManager = client.schema();
        List<VertexLabel> vertexLabels = schemaManager.getVertexLabels();
        List<EdgeLabel> edgeLabels = schemaManager.getEdgeLabels();
        String schema = JsonUtil.toJson(this.getBigModelSchema(vertexLabels, edgeLabels));
        log.info("langchain schema:{}", schema);

        String filePath;
        if (StringUtils.isNotEmpty(requestLangChainParams.fileName)) {
            URL url = LangChainController.class.getClassLoader().getResource("");
            filePath = String.format("%s%s", url.getPath(), requestLangChainParams.fileName);
            this.judgeFileExist(filePath);
            log.info("LangChainController filePath:{}", filePath);
        } else {
            throw new RuntimeException("fileName must not be null");
        }

        List<String> result = this.excutePythonRuntime(requestLangChainParams.pythonPath,
                filePath, requestLangChainParams.query, requestLangChainParams.openKey, schema,
                requestLangChainParams.model, requestLangChainParams.ernieClientId, requestLangChainParams.ernieClientSecret);
        if (CollectionUtils.isEmpty(result)) {
            return this.generateResponseLangChain(requestLangChainParams.query,
                    "LangChain not generate gremlin");
        } else {
            return this.generateResponseLangChain(requestLangChainParams.query,
                    result.get(result.size() - 1));
        }
    }


    @PostMapping("langchain/schema")
    public Object langchainSchema(@PathVariable("graphspace") String graphSpace,
                                  @PathVariable("graph") String graph,
                                  @RequestBody RequestLangChainParams requestLangChainParams) {
        E.checkNotNull(requestLangChainParams, "params must not be null");
        log.info("LangChainController langchain params:{}",
                 requestLangChainParams);
        this.checkUserParam(requestLangChainParams);

        this.tryLogin(graphSpace, graph,
                requestLangChainParams.userName, requestLangChainParams.password);

        HugeClient client = this.authClient(graphSpace, graph);
        SchemaManager schemaManager = client.schema();
        List<VertexLabel> vertexLabels = schemaManager.getVertexLabels();
        List<EdgeLabel> edgeLabels = schemaManager.getEdgeLabels();
        HashMap<String, Object> schema = this.getBigModelSchema(vertexLabels, edgeLabels);
        return schema;
    }

    @PostMapping("gremlin")
    public Object gremlin(@PathVariable("graphspace") String graphSpace,
                          @PathVariable("graph") String graph,
                          @RequestBody RequestLangChainParams requestLangChainParams) {
        E.checkNotNull(requestLangChainParams, "params must not be null");
        GremlinQuery query = new GremlinQuery();
        query.setContent(requestLangChainParams.query);
        this.checkParamsValid(query);
        this.checkUserParam(requestLangChainParams);

        this.tryLogin(graphSpace, graph,
                requestLangChainParams.userName, requestLangChainParams.password);

        try {
            HugeClient client = this.authClient(graphSpace, graph);
            JsonView result =
                    this.queryService.executeSingleGremlinQuery(client, query);
            return result.getData();
        } catch (Throwable e) {
            throw e;
        }
    }

    @PostMapping("langchain_no_schema")
    public Object langchainNoSchema(@PathVariable("graphspace") String graphSpace,
                                    @PathVariable("graph") String graph,
                                    @RequestBody RequestLangChainParams requestLangChainParams) {
        E.checkNotNull(requestLangChainParams, "params must not be null");
        log.info("LangChainController langchain params:{}",
                 requestLangChainParams);

        this.checkParams(requestLangChainParams);
        this.checkModelParams(requestLangChainParams);

        String filePath;
        if (StringUtils.isNotEmpty(requestLangChainParams.fileName)) {
            URL url = LangChainController.class.getClassLoader().getResource("");
            filePath = String.format("%s%s", url.getPath(), requestLangChainParams.fileName);
            this.judgeFileExist(filePath);
            log.info("LangChainController filePath:{}", filePath);
        } else {
            throw new RuntimeException("fileName must not be null");
        }

        List<String> result =
                this.excutePythonByProcessBuilder(
                        requestLangChainParams.pythonPath, filePath,
                        requestLangChainParams.query, requestLangChainParams.openKey,
                        requestLangChainParams.graphSchema, requestLangChainParams.model,
                        requestLangChainParams.ernieClientId, requestLangChainParams.ernieClientSecret);
        if (CollectionUtils.isEmpty(result)) {
            return this.generateResponseLangChain(requestLangChainParams.query,
                    "LangChain not generate gremlin");
        } else {
            return this.generateResponseLangChain(requestLangChainParams.query,
                    result.get(result.size() - 1));
        }
    }

    private void tryLogin(String graphSpace, String graph,
                          String username, String password) {
        log.info("Attempting to login username:{}", username);

        E.checkNotNull(username, "username cannot be null");
        E.checkNotNull(password, "password cannot be null");
        String token = this.getToken();
        if (StringUtils.isNotEmpty(token)) {
            log.info("Attempting to login token exist, username:{} token:{}", username, token);
            return;
        }
        //uuapLoginController.loginVerifyUser(graphSpace, graph, username, password, null);
        // TODO C Deleted
        if (Objects.isNull(this.getToken())) {
            log.error("Attempting to login failed, username:{}", username);
            throw new IllegalStateException("login failed");
        }
    }

    /**
     *
     * @param pythonPath
     * @param pythonScriptPath
     * @param query
     * @param openKey
     * @param graphSchema
     * @return
     */
    private List<String> excutePythonRuntime(String pythonPath,
                                             String pythonScriptPath,
                                             String query,
                                             String openKey,
                                             String graphSchema,
                                             String model,
                                             String ernieClientId,
                                             String ernieClientSecret) {
        String[] args1 = this.getExcuteArgs(pythonPath, pythonScriptPath, query, openKey, graphSchema,
                model, ernieClientId, ernieClientSecret);
        log.info("lang chain execute python command:\n [{}] \n", String.join(" ", args1));

        // 执行Python文件，并传入参数
        List<String> lineList = new ArrayList<>();
        try {
            Process proc = Runtime.getRuntime().exec(args1);
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                lineList.add(line);
                log.info("execute ret:{}", line);
            }
            if (!this.judgeResultSuccess(lineList, model)) {
                this.calculateError(lineList, model);
                log.error("excutePython lineList:{}", lineList);
                lineList.clear();
            } else {
                lineList = getGremlinResults(lineList, model);
            }
            in.close();
            proc.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("excutePythonRuntime error", e);
        }
        return lineList;
    }


    /**
     * 使用ProcessBuilder执行python脚本
     * @param pythonPath
     * @param pythonScriptPath
     * @param query
     * @param openKey
     * @param graphSchema
     * @return
     */
    private List<String> excutePythonByProcessBuilder(String pythonPath,
                                                      String pythonScriptPath,
                                                      String query,
                                                      String openKey,
                                                      String graphSchema,
                                                      String model,
                                                      String ernieClientId,
                                                      String ernieClientSecret) {
        String[] args1 = this.getExcuteArgs(pythonPath, pythonScriptPath, query, openKey, graphSchema,
                model, ernieClientId, ernieClientSecret);

        // 构造ProcessBuilder对象
        ProcessBuilder pb = new ProcessBuilder(args1);

        // 将python脚本的输出和错误输出合并
        pb.redirectErrorStream(false);

        List<String> lineList = new ArrayList<>();
        try {
            // 启动进程
            Process process = pb.start();
            // 读取进程的输出
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String ret;
            while ((ret = in.readLine()) != null) {
                lineList.add(ret);
                log.info("execute ret:{}", ret);
            }
            if (!this.judgeResultSuccess(lineList, model)) {
                this.calculateError(lineList, model);
                log.error("excutePython lineList:{}", lineList);
                lineList.clear();
            } else {
                lineList = getGremlinResults(lineList, model);
            }
            // 等待进程结束
            int exitCode = process.waitFor();
            log.info("Exited with error code : " + exitCode);
        } catch (Exception e) {
            log.error("excutePythonRuntime error", e);
        }
        return lineList;
    }

    private void judgeFileExist(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.error("python file not exist:{}", filePath);
            throw new IllegalArgumentException("python file not exist");
        }
    }

    private List<String> getGremlinResults(List<String> lineList, String model) {
        if (GPT_4_MODEL.equals(model)) {
            return Arrays.asList(this.getGpt4GremlinResults(lineList));
        } else if (WENXIN_4_MODEL.equals(model)) {
            return Arrays.asList(this.getWenXin4GremlinResults(lineList));
        }
        return Collections.emptyList();
    }

    private String getGpt4GremlinResults(List<String> lineList) {
        for (String line : lineList) {
            String tmp = line.toLowerCase();
            if (tmp.contains(G_V) || tmp.contains(G_E)) {
                if (!line.startsWith(G_V) || !line.startsWith(G_E)) {
                    line = cutGremlin(line);
                }
                return line;
            }
        }
        return StringUtils.EMPTY;
    }

    private String  getWenXin4GremlinResults(List<String> lineList) {
        for (String line : lineList) {
            String tmp = line.toLowerCase();
            if (tmp.contains(G_V) || tmp.contains(G_E)) {
                if (!line.startsWith(G_V) || !line.startsWith(G_E)) {
                    line = cutGremlin(line);
                }
                return line;
            }
        }
        return StringUtils.EMPTY;
    }

    private String cutGremlin(String line) {
        int start = 0;
        int end = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == 'g') {
                start = i;
                break;
            }
        }
        for (int i = line.length() - 1; i >= 0; i--) {
            if (line.charAt(i) == ')') {
                end = i + 1;
                break;
            }
        }
        return line.substring(start, end);
    }

    /**
     * 判断结果是否正确
     * @param lineList
     * @return
     */
    private boolean judgeResultSuccess(List<String> lineList, String model) {
        if (GPT_4_MODEL.equals(model)) {
            return this.judgeGpt4ResultSuccess(lineList);
        } else if (WENXIN_4_MODEL.equals(model)) {
            return this.judgeWenXin4ResultSuccess(lineList);
        }
        return false;
    }

    private boolean judgeGpt4ResultSuccess(List<String> lineList) {
        if (CollectionUtils.isEmpty(lineList)) {
            return false;
        }
        for (String line : lineList) {
            if (line.contains("Finished chain") ||
                line.toLowerCase().contains(G_E) ||
                line.toLowerCase().contains(G_V)) {
                return true;
            }
        }
        return false;
    }

    private boolean judgeWenXin4ResultSuccess(List<String> lineList) {
        if (CollectionUtils.isEmpty(lineList)) {
            return false;
        }
        for (String line : lineList) {
            if (line.contains("```") || line.toLowerCase().contains(G_E) ||
                line.toLowerCase().contains(G_V)) {
                return true;
            }
        }
        return true;
    }

    /**
     * 输出error 信息
     * @param proc
     * @return
     */
    private void calculateError(List<String> proc, String model) {
        if (GPT_4_MODEL.equals(model)) {
            this.calculateGpt4Error(proc);
        } else if (WENXIN_4_MODEL.equals(model)) {
            this.calculateWenXin4Error(proc);
        }
    }

    private void calculateGpt4Error(List<String> proc) {
        try {
            StringBuilder sb = new StringBuilder();
            for (String line : proc) {
                sb.append(line).append("\n");
            }
            log.error("calculateError error {}", sb.toString());
        } catch (Exception e) {
            log.error("calculateError error", e);
        }
    }

    private void calculateWenXin4Error(List<String> proc) {
        try {
            StringBuilder sb = new StringBuilder();
            for (String line : proc) {
                sb.append(line).append("\n");
            }
            log.error("calculateError error {}", sb.toString());
        } catch (Exception e) {
            log.error("calculateError error", e);
        }
    }

    /**
     * 生成返回结果
     * @param query
     * @param gremlin
     * @return
     */
    private ResponseLangChain generateResponseLangChain(String query, String gremlin) {
        return ResponseLangChain.builder().gremlin(gremlin).query(query).build();
    }

    private void checkUserParam(RequestLangChainParams requestLangChainParams) {
        E.checkNotNull(requestLangChainParams.getUserName(), "params username must not be null");
        E.checkNotNull(requestLangChainParams.getPassword(), "params password must not be null");
    }
    private void checkParams(RequestLangChainParams requestLangChainParams) {
        E.checkNotNull(requestLangChainParams, "params must not be null");
        E.checkNotNull(requestLangChainParams.getQuery(), "params query must not be null");
        E.checkNotNull(requestLangChainParams.getModel(), "params model must not be null");
        E.checkState(DEFAULT_MODEL.contains(requestLangChainParams.getModel()), "mode must be [\"wenxin4\", \"gpt4\"]");
    }

    private void checkModelParams(RequestLangChainParams requestLangChainParams) {
        if (GPT_4_MODEL.equals(requestLangChainParams.getModel())) {
            E.checkNotNull(requestLangChainParams.getOpenKey(), "params open_key must not be null");
        }
        if (WENXIN_4_MODEL.equals(requestLangChainParams.getModel())) {
            E.checkNotNull(requestLangChainParams.getErnieClientId(), "params ernie_client_id must not be null");
            E.checkNotNull(requestLangChainParams.getErnieClientSecret(), "params ernie_client_secret must not be null");
        }
    }

    private String[] getExcuteArgs(String pythonPath,
                                   String pythonScriptPath,
                                   String query,
                                   String openKey,
                                   String graphSchema,
                                   String model,
                                   String ernieClientId,
                                   String ernieClientSecret) {
        List<String> argsList = new ArrayList<>();
        argsList.add(pythonPath);
        argsList.add(pythonScriptPath);
        argsList.add("--query");
        argsList.add(query);
        argsList.add("--graph_schema");
        argsList.add(graphSchema);
        if (WENXIN_4_MODEL.equals(model)) {
            if (ernieClientSecret != null) {
                argsList.add("--ernie_client_secret");
                argsList.add(ernieClientSecret);
            }
            if (ernieClientId != null) {
                argsList.add("--ernie_client_id");
                argsList.add(ernieClientId);
            }
        } else if (GPT_4_MODEL.equals(model)) {
            if (openKey != null) {
                argsList.add("--open_key");
                argsList.add(openKey);
            }
        }
        argsList.add("--model");
        argsList.add(model);
        return argsList.toArray(new String[argsList.size()]);
    }

    private void checkParamsValid(GremlinQuery query) {
        Ex.check(!StringUtils.isEmpty(query.getContent()),
                "common.param.cannot-be-null-or-empty",
                "gremlin-query.content");
        Ex.check(query.getContent().length() <= GremlinController.CONTENT_LENGTH_LIMIT,
                "gremlin.statement.exceed-limit", GremlinController.CONTENT_LENGTH_LIMIT);
    }

    private HashMap<String, Object> getBigModelSchema(List<VertexLabel> vertexLabels, List<EdgeLabel> edgeLabels) {
        List<VertexLabelVo> vertexLabelVoList = Lists.newArrayList();
        List<EdgeLabelVo> edgeLabelVoList = Lists.newArrayList();
        List<String> relationshipsList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(vertexLabels)) {
            for (VertexLabel vertexLabel : vertexLabels) {
                VertexLabelVo vertexLabelVo = new VertexLabelVo();
                vertexLabelVo.setName(vertexLabel.name());
                vertexLabelVo.setPrimaryKeys(vertexLabel.primaryKeys());
                vertexLabelVo.setProperties(Lists.newArrayList());
                vertexLabelVo.getProperties().addAll(vertexLabel.properties());

                vertexLabelVoList.add(vertexLabelVo);
            }
        }

        if (CollectionUtils.isNotEmpty(edgeLabels)) {
            for (EdgeLabel edgeLabel : edgeLabels) {
                EdgeLabelVo edgeLabelVo = new EdgeLabelVo();
                edgeLabelVo.setName(edgeLabel.name());
                edgeLabelVo.setProperties(Lists.newArrayList());
                edgeLabelVo.getProperties().addAll(edgeLabel.properties());

                edgeLabelVoList.add(edgeLabelVo);

                relationshipsList.add(Relationship.getRelation(edgeLabel.sourceLabel(),
                        edgeLabel.name(), edgeLabel.targetLabel()));
            }
        }

        HashMap<String, Object> schema = new HashMap<String, Object>();
        schema.put("Node properties", vertexLabelVoList);
        schema.put("Edge properties", edgeLabelVoList);
        schema.put("Relationships", relationshipsList);
        return schema;
    }



    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class RequestLangChainParams {
        @JsonProperty("python_path")
        private String pythonPath = "python";

        @JsonProperty("file_name")
        private String fileName = DEFAULT_PYTHON_FILE;

        @JsonProperty("query")
        private String query;

        @JsonProperty("open_key")
        private String openKey;


        @JsonProperty("graph_schema")
        private String graphSchema = "";

        @JsonProperty("ernie_client_secret")
        private String ernieClientSecret;

        @JsonProperty("ernie_client_id")
        private String ernieClientId;

        @JsonProperty("model")
        private String model = "wenxin4";

        @JsonProperty("username")
        private String userName;

        @JsonProperty("password")
        private String password;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class ResponseLangChain {
        @JsonProperty("query")
        private String query;

        @JsonProperty("gremlin")
        private String gremlin;
    }



    @Data
    static class VertexLabelVo {

        @JsonProperty("name")
        private String name;

        @JsonProperty("primary_keys")
        private List<String> primaryKeys;

        @JsonProperty("properties")
        private List<String> properties;
    }

    @Data
    static class EdgeLabelVo {
        @JsonProperty("name")
        private String name;

        @JsonProperty("properties")
        private List<String> properties;
    }

    static class Relationship {
        public static String getRelation(String sourceLabel, String name, String targetLabel) {
            return sourceLabel + "--" + name + "-->" + targetLabel;

        }
    }
}
