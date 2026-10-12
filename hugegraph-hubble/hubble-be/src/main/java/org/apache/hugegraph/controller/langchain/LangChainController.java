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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.hugegraph.controller.query.GremlinController;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.entity.query.GremlinQuery;
import org.apache.hugegraph.entity.query.JsonView;
import org.apache.hugegraph.service.query.QueryService;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.config.ConfigOption;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.options.HubbleOptions;
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
    private static final String DEFAULT_PYTHON_SCRIPT = "excute_langchain.py";

    private static final String G_V = "g.v";
    private static final String G_E = "g.e";

    private static final String WENXIN_4_MODEL = "wenxin4";
    private static final String GPT_4_MODEL = "gpt4";

    private static final List<String> DEFAULT_MODEL = Arrays.asList(WENXIN_4_MODEL, GPT_4_MODEL);

    @Autowired
    private QueryService queryService;

    @Autowired
    private HugeConfig config;

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
        log.info("LangChainController langchain request model:{} file:{}",
                 requestLangChainParams.model, requestLangChainParams.fileName);
        this.checkParams(requestLangChainParams);

        return this.langChainQuery(graphSpace, graph, requestLangChainParams);
    }

    private ResponseLangChain langChainQuery(String graphSpace, String graph,
                                             RequestLangChainParams requestLangChainParams) {
        HugeClient client = this.authClient(graphSpace, graph);
        SchemaManager schemaManager = client.schema();
        List<VertexLabel> vertexLabels = schemaManager.getVertexLabels();
        List<EdgeLabel> edgeLabels = schemaManager.getEdgeLabels();
        String schema = JsonUtil.toJson(this.getBigModelSchema(vertexLabels, edgeLabels));
        log.info("langchain schema:{}", schema);

        String filePath = this.resolvePythonScriptPath(
                          requestLangChainParams.fileName);
        log.info("LangChainController filePath:{}", filePath);

        List<String> result =
                this.excutePythonRuntime(requestLangChainParams.pythonPath,
                                         filePath, requestLangChainParams.query,
                                         requestLangChainParams.openKey, schema,
                                         requestLangChainParams.model,
                                         requestLangChainParams.ernieClientId,
                                         requestLangChainParams.ernieClientSecret);
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
        log.info("LangChainController langchain schema request username:{}",
                 requestLangChainParams.userName);
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
            HugeClient client = this.authGremlinClient(graphSpace, graph);
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
        log.info("LangChainController langchain no schema request model:{} file:{}",
                 requestLangChainParams.model, requestLangChainParams.fileName);

        this.checkParams(requestLangChainParams);
        this.checkModelParams(requestLangChainParams);

        String filePath = this.resolvePythonScriptPath(
                          requestLangChainParams.fileName);
        log.info("LangChainController filePath:{}", filePath);

        List<String> result =
                this.excutePythonByProcessBuilder(
                        requestLangChainParams.pythonPath, filePath,
                        requestLangChainParams.query,
                        requestLangChainParams.openKey,
                        requestLangChainParams.graphSchema,
                        requestLangChainParams.model,
                        requestLangChainParams.ernieClientId,
                        requestLangChainParams.ernieClientSecret);
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
            log.info("Attempting to login token exist, username:{}", username);
            return;
        }
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
        String[] args1 = this.getExcuteArgs(pythonPath, pythonScriptPath,
                                            query, openKey, graphSchema,
                                            model, ernieClientId,
                                            ernieClientSecret);
        return this.executePythonProcess(args1, model,
                                         this.secretValues(openKey,
                                                           ernieClientSecret));
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
        String[] args1 = this.getExcuteArgs(pythonPath, pythonScriptPath,
                                            query, openKey, graphSchema,
                                            model, ernieClientId,
                                            ernieClientSecret);
        return this.executePythonProcess(args1, model,
                                         this.secretValues(openKey,
                                                           ernieClientSecret));
    }

    private String resolvePythonScriptPath(String fileName) {
        E.checkArgument(StringUtils.isNotBlank(fileName),
                        "fileName must not be blank");
        Path requested = Paths.get(fileName).normalize();
        E.checkArgument(!requested.isAbsolute() &&
                        requested.getFileName() != null &&
                        (requested.getNameCount() == 1 ||
                         DEFAULT_PYTHON_FILE.equals(requested.toString())),
                        "python file is not allowed");

        String scriptName = requested.getFileName().toString();
        List<String> allowlist =
                this.configValue(HubbleOptions.LANGCHAIN_SCRIPT_ALLOWLIST);
        
        String safeScriptName = null;
        for (String allowed : allowlist) {
            if (allowed.equals(scriptName)) {
                safeScriptName = allowed;
                break;
            }
        }
        if (safeScriptName == null) {
            throw new IllegalArgumentException("python file is not allowed");
        }

        Path root = Paths.get(this.configValue(HubbleOptions.LANGCHAIN_SCRIPT_DIR))
                         .normalize();
        if (!root.toFile().isDirectory()) {
            throw new IllegalArgumentException("langchain script dir not exist");
        }
        Path script = root.resolve(safeScriptName).normalize();
        if (!script.startsWith(root)) {
            throw new IllegalArgumentException("python file is not allowed");
        }
        if (!script.toFile().exists()) {
            throw new IllegalArgumentException("python file not exist");
        }
        return script.toString();
    }

    private List<String> executePythonProcess(String[] args, String model,
                                              Set<String> secrets) {
        log.info("lang chain execute python command: {}",
                 this.sanitizeCommandForLog(args));

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(false);

        List<String> lineList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();
        Process process;
        try {
            process = pb.start();
            Thread stdout = this.readProcessStream(process.getInputStream(),
                                                   lineList, "stdout", secrets);
            Thread stderr = this.readProcessStream(process.getErrorStream(),
                                                   errorList, "stderr", secrets);
            int timeout = this.configValue(HubbleOptions.LANGCHAIN_EXECUTE_TIMEOUT);
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("LangChain python execution timeout");
            }
            stdout.join(TimeUnit.SECONDS.toMillis(1L));
            stderr.join(TimeUnit.SECONDS.toMillis(1L));

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("LangChain python stderr:{}",
                          this.redactLines(errorList, secrets));
                throw new IllegalStateException(
                          "LangChain python exited with code " + exitCode);
            }

            if (!this.judgeResultSuccess(lineList, model)) {
                List<String> redactedLines = this.redactLines(lineList, secrets);
                this.calculateError(redactedLines, model);
                log.error("excutePython lineList:{}", redactedLines);
                lineList.clear();
            } else {
                lineList = getGremlinResults(lineList, model);
            }
        } catch (IOException e) {
            throw new IllegalStateException("LangChain python execution failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LangChain python execution interrupted", e);
        }
        return lineList;
    }

    private Thread readProcessStream(InputStream stream, List<String> lines,
                                     String streamName, Set<String> secrets) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                    log.info("execute {} ret:{}", streamName,
                             this.redact(line, secrets));
                }
            } catch (IOException e) {
                log.error("Read LangChain process {} failed", streamName, e);
            }
        }, "langchain-" + streamName + "-reader");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private Set<String> secretValues(String openKey, String ernieClientSecret) {
        Set<String> secrets = new java.util.HashSet<>();
        if (StringUtils.isNotEmpty(openKey)) {
            secrets.add(openKey);
        }
        if (StringUtils.isNotEmpty(ernieClientSecret)) {
            secrets.add(ernieClientSecret);
        }
        return secrets;
    }

    private List<String> redactLines(List<String> lines, Set<String> secrets) {
        List<String> redacted = new ArrayList<>(lines.size());
        for (String line : lines) {
            redacted.add(this.redact(line, secrets));
        }
        return redacted;
    }

    private String redact(String value, Set<String> secrets) {
        String redacted = value;
        for (String secret : secrets) {
            redacted = StringUtils.replace(redacted, secret, "******");
        }
        return redacted;
    }

    private String sanitizeCommandForLog(String[] args) {
        List<String> sanitized = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            sanitized.add(args[i]);
            if ("--open_key".equals(args[i]) ||
                "--ernie_client_secret".equals(args[i])) {
                if (i + 1 < args.length) {
                    sanitized.add("******");
                    i++;
                }
            }
        }
        return String.join(" ", sanitized);
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
        E.checkNotNull(requestLangChainParams.getUserName(),
                       "params username must not be null");
        E.checkNotNull(requestLangChainParams.getPassword(),
                       "params password must not be null");
    }

    private void checkParams(RequestLangChainParams requestLangChainParams) {
        E.checkNotNull(requestLangChainParams, "params must not be null");
        E.checkNotNull(requestLangChainParams.getQuery(), "params query must not be null");
        E.checkNotNull(requestLangChainParams.getModel(), "params model must not be null");
        E.checkState(DEFAULT_MODEL.contains(requestLangChainParams.getModel()),
                     "mode must be [\"wenxin4\", \"gpt4\"]");
    }

    private void checkModelParams(RequestLangChainParams requestLangChainParams) {
        if (GPT_4_MODEL.equals(requestLangChainParams.getModel())) {
            E.checkNotNull(requestLangChainParams.getOpenKey(),
                           "params open_key must not be null");
        }
        if (WENXIN_4_MODEL.equals(requestLangChainParams.getModel())) {
            E.checkNotNull(requestLangChainParams.getErnieClientId(),
                           "params ernie_client_id must not be null");
            E.checkNotNull(requestLangChainParams.getErnieClientSecret(),
                           "params ernie_client_secret must not be null");
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
        argsList.add(this.configValue(HubbleOptions.LANGCHAIN_PYTHON_PATH));
        argsList.add(pythonScriptPath);
        argsList.add("--query");
        argsList.add(query);
        argsList.add("--graph_schema");
        argsList.add(StringUtils.defaultString(graphSchema));
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

    private <T> T configValue(ConfigOption<T> option) {
        if (this.config == null) {
            return option.defaultValue();
        }
        return this.config.get(option);
    }

    private void checkParamsValid(GremlinQuery query) {
        Ex.check(!StringUtils.isEmpty(query.getContent()),
                "common.param.cannot-be-null-or-empty",
                "gremlin-query.content");
        Ex.check(query.getContent().length() <= GremlinController.CONTENT_LENGTH_LIMIT,
                "gremlin.statement.exceed-limit", GremlinController.CONTENT_LENGTH_LIMIT);
    }

    private HashMap<String, Object> getBigModelSchema(List<VertexLabel> vertexLabels,
                                                      List<EdgeLabel> edgeLabels) {
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
