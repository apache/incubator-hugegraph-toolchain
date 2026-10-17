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

package org.apache.hugegraph.service.query;

import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.entity.enums.QueryLanguage;
import org.apache.hugegraph.entity.query.Text2GqlRequest;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.util.JsonUtil;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;

@Service
public class Text2GqlService {

    private static final byte[] AWS_IPV6_METADATA_ADDRESS = {
        (byte) 0xfd, 0, 0x0e, (byte) 0xc2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x02, 0x54
    };
    private static final Pattern CODE_BLOCK = Pattern.compile(
            "\\A```([A-Za-z]*)[ \\t]*\\R([\\s\\S]*?)\\R```\\z");

    private final Duration timeout;

    public Text2GqlService() {
        this(Duration.ofSeconds(60));
    }

    Text2GqlService(Duration timeout) {
        this.timeout = timeout;
    }

    public String generate(HugeClient client, Text2GqlRequest request) {
        if (request == null) {
            throw new ExternalException("text2gql.invalid-request");
        }
        QueryLanguage language = request.getLanguage();
        if (language == null) {
            throw new ExternalException("text2gql.invalid-language");
        }
        language.requireSupported(client);
        String question = required(request.getQuestion(), 4000);
        String model = required(request.getModel(), 256);
        String baseUrl = validateBaseUrl(required(request.getBaseUrl(), 2048));
        String key = request.getApiKey() == null ? "" : request.getApiKey();
        if (key.length() > 4096 || key.chars().anyMatch(Character::isISOControl)) {
            throw new ExternalException("text2gql.invalid-request");
        }
        key = key.trim();
        String schema = schema(client);
        String result;
        try {
            OpenAiChatModel chat = OpenAiChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(key.isEmpty() ? "ollama" : key)
                    .modelName(model)
                    .timeout(this.timeout)
                    .maxRetries(0)
                    .logRequests(false)
                    .logResponses(false)
                    .httpClientBuilder(new Text2GqlHttpClient(host -> resolveAndValidate(
                            host, "http".equalsIgnoreCase(URI.create(baseUrl).getScheme()))))
                    .build();
            result = chat.chat(SystemMessage.from(language.instructions() + schema),
                               UserMessage.from(question)).aiMessage().text();
        } catch (RuntimeException exception) {
            // Do not retain upstream bodies, credentials or SDK exceptions in application logs.
            if (isTimeout(exception)) {
                throw new ExternalException(504, "text2gql.model-timeout");
            }
            throw new ExternalException(502, "text2gql.model-failed");
        }
        return normalize(result, language);
    }

    static String normalize(String result, QueryLanguage language) {
        if (result == null || result.length() > 10240) {
            throw new ExternalException(502, "text2gql.invalid-output");
        }
        String query = result.trim();
        Matcher matcher = CODE_BLOCK.matcher(query);
        if (matcher.matches()) {
            if (!language.acceptsCodeFence(matcher.group(1))) {
                throw new ExternalException(502, "text2gql.invalid-output");
            }
            query = matcher.group(2).trim();
        }
        try {
            language.validate(query);
        } catch (RuntimeException exception) {
            throw new ExternalException(502, "text2gql.invalid-output");
        }
        return query;
    }

    private static String required(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new ExternalException("text2gql.invalid-request");
        }
        return value.trim();
    }

    static String validateBaseUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            boolean http = "http".equalsIgnoreCase(uri.getScheme());
            if ((!http && !"https".equalsIgnoreCase(uri.getScheme())) || host == null ||
                uri.getUserInfo() != null || uri.getRawQuery() != null ||
                uri.getRawFragment() != null || uri.getPort() == 0 || uri.getPort() > 65535 ||
                host.matches("[0-9]+") || host.equalsIgnoreCase("metadata.google.internal") ||
                host.equalsIgnoreCase("metadata")) {
                throw new IllegalArgumentException();
            }
            resolveAndValidate(host, http);
            return stripTrailingSlashes(value);
        } catch (Exception exception) {
            throw new ExternalException("text2gql.invalid-base-url");
        }
    }

    static List<InetAddress> resolveAndValidate(String host, boolean http) throws UnknownHostException {
        // The transport calls this again at connection time and directly uses these approved addresses.
        List<InetAddress> addresses = List.of(InetAddress.getAllByName(host));
        for (InetAddress address : addresses) {
            if (address.isAnyLocalAddress() || address.isLinkLocalAddress() ||
                address.isMulticastAddress() || isMetadataOrReserved(address) ||
                (http && !address.isLoopbackAddress() && !address.isSiteLocalAddress() &&
                 !isUniqueLocal(address))) {
                throw new IllegalArgumentException("Model address was rejected");
            }
        }
        return addresses;
    }

    static String stripTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private static boolean isMetadataOrReserved(InetAddress address) {
        byte[] bytes = address.getAddress();
        return Arrays.equals(bytes, AWS_IPV6_METADATA_ADDRESS) ||
               (bytes.length == 4 && (bytes[0] == 0 || (bytes[0] & 0xff) == 255 ||
                                      "100.100.100.200".equals(address.getHostAddress())));
    }

    private static boolean isUniqueLocal(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }

    private static boolean isTimeout(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof java.net.SocketTimeoutException ||
                (cause instanceof InterruptedIOException && "timeout".equals(cause.getMessage()))) {
                return true;
            }
        }
        return false;
    }

    static String schema(HugeClient client) {
        String json;
        try {
            SchemaManager schema = client.schema();
            List<Map<String, Object>> propertyKeys = new ArrayList<>();
            for (PropertyKey key : schema.getPropertyKeys()) {
                propertyKeys.add(Map.of("name", key.name(), "data_type", key.dataType(),
                                        "cardinality", key.cardinality()));
            }
            List<Map<String, Object>> vertices = new ArrayList<>();
            for (VertexLabel label : schema.getVertexLabels()) {
                vertices.add(Map.of("name", label.name(), "id_strategy", label.idStrategy(),
                                    "primary_keys", label.primaryKeys(), "properties", label.properties(),
                                    "nullable_keys", label.nullableKeys()));
            }
            List<Map<String, Object>> edges = new ArrayList<>();
            for (EdgeLabel label : schema.getEdgeLabels()) {
                Map<String, Object> edge = new LinkedHashMap<>();
                edge.put("name", label.name());
                edge.put("links", label.links());
                edge.put("edge_label_type", label.edgeLabelType());
                edge.put("parent_label", label.parentLabel());
                edge.put("properties", label.properties());
                edge.put("sort_keys", label.sortKeys());
                edge.put("nullable_keys", label.nullableKeys());
                edges.add(edge);
            }
            json = JsonUtil.toJson(Map.of("property_keys", propertyKeys,
                                         "vertex_labels", vertices, "edge_labels", edges));
        } catch (RuntimeException exception) {
            throw new ExternalException(502, "text2gql.schema-failed");
        }
        if (json.length() > 65536) {
            throw new ExternalException("text2gql.schema-too-large");
        }
        return json;
    }
}
