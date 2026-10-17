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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.entity.query.Text2GqlConfiguration;
import org.apache.hugegraph.entity.query.Text2GqlRequest;
import org.apache.hugegraph.entity.query.Text2GqlSettingsRequest;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.mapper.query.Text2GqlConfigMapper;
import org.apache.hugegraph.options.HubbleOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Text2GqlSettingsService {

    private final HugeConfig config;
    private final Text2GqlConfigMapper mapper;
    private final Text2GqlSecretStore secrets;
    private final Function<String, String> environment;

    @Autowired
    public Text2GqlSettingsService(HugeConfig config, Text2GqlConfigMapper mapper,
                                   Text2GqlSecretStore secrets) {
        this(config, mapper, secrets, System::getenv);
    }

    Text2GqlSettingsService(HugeConfig config, Text2GqlConfigMapper mapper,
                            Text2GqlSecretStore secrets, Function<String, String> environment) {
        this.config = config;
        this.mapper = mapper;
        this.secrets = secrets;
        this.environment = environment;
    }

    public boolean customAvailable(boolean anonymous) {
        return !anonymous || Boolean.TRUE.equals(
                this.config.get(HubbleOptions.TEXT2GQL_CUSTOM_ALLOW_ANONYMOUS));
    }

    public Map<String, Object> availability(boolean anonymous) {
        if (anonymous && !this.allowAnonymous()) {
            return Map.of("available", false, "reason", "authentication_required");
        }
        try {
            Snapshot current = this.current();
            if (!current.configuration.isEnabled()) {
                return Map.of("available", false, "reason", "not_configured");
            }
            Text2GqlRequest resolved = this.connection(current, "");
            return Map.of("available", true, "model", resolved.getModel());
        } catch (RuntimeException exception) {
            return Map.of("available", false, "reason", "unavailable");
        }
    }

    public Text2GqlRequest sharedRequest(Text2GqlRequest request, boolean anonymous) {
        if (request == null || !"shared".equals(request.getSource()) ||
            hasText(request.getBaseUrl()) || hasText(request.getModel()) || hasText(request.getApiKey())) {
            throw new ExternalException("text2gql.shared.mixed-configuration");
        }
        if (anonymous && !this.allowAnonymous()) {
            throw new ExternalException(403, "text2gql.shared.authentication-required");
        }
        Snapshot current = this.current();
        if (!current.configuration.isEnabled()) {
            throw new ExternalException(503, "text2gql.shared.not-configured");
        }
        if (request.getLanguage() == null) {
            throw new ExternalException("text2gql.invalid-language");
        }
        Text2GqlRequest resolved = this.connection(current, request.getQuestion());
        resolved.setLanguage(request.getLanguage());
        return resolved;
    }

    /** Administrator-only descriptor. This deliberately never decrypts or serializes a secret. */
    public Map<String, Object> settings() {
        return this.descriptor(this.current());
    }

    /** Replace the complete snapshot in one statement; never merge a client URL with a server secret. */
    public synchronized Map<String, Object> save(Text2GqlSettingsRequest request) {
        if (request == null || request.getEnabled() == null) {
            throw new ExternalException("text2gql.shared.invalid-settings");
        }
        String type = optional(request.getConnectionType(), 16);
        if (!"openai".equals(type) && !"ollama".equals(type)) {
            throw new ExternalException("text2gql.shared.invalid-settings");
        }
        String baseUrl = optional(request.getBaseUrl(), 2048);
        String model = optional(request.getModel(), 256);
        if (!baseUrl.isEmpty()) {
            baseUrl = request.getEnabled() ? Text2GqlService.validateBaseUrl(baseUrl) : normalized(baseUrl);
        }
        if (request.getEnabled() && (baseUrl.isEmpty() || model.isEmpty())) {
            throw new ExternalException("text2gql.shared.invalid-settings");
        }
        String suppliedKey = apiKey(request.getApiKey());
        if (request.isClearApiKey() && !suppliedKey.isEmpty()) {
            throw new ExternalException("text2gql.shared.invalid-settings");
        }
        Snapshot previous = this.current();
        boolean sameUrl = baseUrl.equals(normalized(previous.configuration.getBaseUrl()));
        boolean previousHasKey = this.hasKey(previous);
        if (!sameUrl && previousHasKey && suppliedKey.isEmpty() && !request.isClearApiKey()) {
            throw new ExternalException("text2gql.shared.new-key-required");
        }
        String encrypted = "";
        if (!suppliedKey.isEmpty()) {
            encrypted = this.secrets.encrypt(suppliedKey);
        } else if (!request.isClearApiKey() && sameUrl && previousHasKey) {
            if (previous.managed) {
                encrypted = previous.configuration.getEncryptedApiKey();
                if (request.getEnabled()) {
                    this.secrets.decrypt(encrypted);
                }
            } else {
                String deploymentKey = this.deploymentKey();
                if (!deploymentKey.isEmpty()) {
                    encrypted = this.secrets.encrypt(deploymentKey);
                }
            }
        }
        if (request.getEnabled() && "openai".equals(type) && encrypted.isEmpty()) {
            throw new ExternalException("text2gql.shared.api-key-required");
        }
        Text2GqlConfiguration updated = new Text2GqlConfiguration();
        updated.setEnabled(request.getEnabled());
        updated.setConnectionType(type);
        updated.setBaseUrl(baseUrl);
        updated.setModel(model);
        updated.setEncryptedApiKey(encrypted);
        try {
            this.mapper.save(updated);
        } catch (RuntimeException exception) {
            throw new ExternalException(503, "text2gql.shared.settings-unavailable");
        }
        return this.descriptor(new Snapshot(updated, true));
    }

    private boolean allowAnonymous() {
        return Boolean.TRUE.equals(this.config.get(HubbleOptions.TEXT2GQL_SHARED_ALLOW_ANONYMOUS));
    }

    private Snapshot current() {
        Text2GqlConfiguration managed;
        try {
            managed = this.mapper.current();
        } catch (RuntimeException exception) {
            throw new ExternalException(503, "text2gql.shared.settings-unavailable");
        }
        if (managed != null) {
            return new Snapshot(managed, true);
        }
        Text2GqlConfiguration deployment = new Text2GqlConfiguration();
        deployment.setEnabled(Boolean.TRUE.equals(this.config.get(HubbleOptions.TEXT2GQL_SHARED_ENABLED)));
        deployment.setConnectionType(this.config.get(HubbleOptions.TEXT2GQL_SHARED_CONNECTION_TYPE));
        deployment.setBaseUrl(this.config.get(HubbleOptions.TEXT2GQL_SHARED_BASE_URL));
        deployment.setModel(this.config.get(HubbleOptions.TEXT2GQL_SHARED_MODEL));
        return new Snapshot(deployment, false);
    }

    private Map<String, Object> descriptor(Snapshot snapshot) {
        Text2GqlConfiguration configuration = snapshot.configuration;
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("enabled", configuration.isEnabled());
        descriptor.put("connection_type", hasText(configuration.getConnectionType()) ?
                                          configuration.getConnectionType() : "openai");
        descriptor.put("base_url", normalized(configuration.getBaseUrl()));
        descriptor.put("model", configuration.getModel() == null ? "" : configuration.getModel());
        descriptor.put("has_api_key", this.hasKey(snapshot));
        descriptor.put("source", snapshot.managed ? "managed" : "deployment");
        return descriptor;
    }

    private boolean hasKey(Snapshot snapshot) {
        if (snapshot.managed) {
            return hasText(snapshot.configuration.getEncryptedApiKey());
        }
        if (hasText(this.environment.apply("HUBBLE_TEXT2GQL_API_KEY"))) {
            return true;
        }
        String file = this.config.get(HubbleOptions.TEXT2GQL_SHARED_API_KEY_FILE);
        if (!hasText(file)) {
            return false;
        }
        try {
            Path path = Path.of(file);
            return Files.isRegularFile(path) && Files.size(path) > 0;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private Text2GqlRequest connection(Snapshot snapshot, String question) {
        try {
            Text2GqlConfiguration configuration = snapshot.configuration;
            String type = configuration.getConnectionType();
            if (!"openai".equals(type) && !"ollama".equals(type)) {
                throw new IllegalArgumentException();
            }
            String baseUrl = optional(configuration.getBaseUrl(), 2048);
            String model = optional(configuration.getModel(), 256);
            if (baseUrl.isEmpty() || model.isEmpty()) {
                throw new IllegalArgumentException();
            }
            baseUrl = Text2GqlService.validateBaseUrl(baseUrl);
            String key = snapshot.managed ?
                         (hasText(configuration.getEncryptedApiKey()) ?
                          this.secrets.decrypt(configuration.getEncryptedApiKey()) : "") : this.deploymentKey();
            if ("openai".equals(type) && key.isEmpty()) {
                throw new IllegalArgumentException();
            }
            Text2GqlRequest resolved = new Text2GqlRequest();
            resolved.setQuestion(question);
            resolved.setBaseUrl(baseUrl);
            resolved.setModel(model);
            resolved.setApiKey(key);
            return resolved;
        } catch (RuntimeException exception) {
            throw new ExternalException(503, "text2gql.shared.unavailable");
        }
    }

    private String deploymentKey() {
        try {
            String environmentKey = this.environment.apply("HUBBLE_TEXT2GQL_API_KEY");
            if (hasText(environmentKey)) {
                return apiKey(environmentKey);
            }
            String file = this.config.get(HubbleOptions.TEXT2GQL_SHARED_API_KEY_FILE);
            if (!hasText(file)) {
                return "";
            }
            Path path = Path.of(file);
            if (!Files.isRegularFile(path) || Files.size(path) > 16384) {
                throw new IllegalArgumentException();
            }
            return apiKey(Files.readString(path, StandardCharsets.UTF_8).trim());
        } catch (IOException | RuntimeException exception) {
            throw new ExternalException(503, "text2gql.shared.secret-unavailable");
        }
    }

    private static String apiKey(String key) {
        if (key == null) {
            return "";
        }
        if (key.length() > 4096 || key.chars().anyMatch(Character::isISOControl)) {
            throw new ExternalException("text2gql.shared.invalid-settings");
        }
        return key.trim();
    }

    private static String optional(String value, int limit) {
        if (value == null) {
            return "";
        }
        if (value.length() > limit) {
            throw new ExternalException("text2gql.shared.invalid-settings");
        }
        return value.trim();
    }

    private static String normalized(String value) {
        return value == null ? "" : Text2GqlService.stripTrailingSlashes(value.trim());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static final class Snapshot {

        private final Text2GqlConfiguration configuration;
        private final boolean managed;

        private Snapshot(Text2GqlConfiguration configuration, boolean managed) {
            this.configuration = configuration;
            this.managed = managed;
        }
    }
}
