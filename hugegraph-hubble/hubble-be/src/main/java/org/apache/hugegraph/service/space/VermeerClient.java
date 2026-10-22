/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.service.space;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.util.E;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VermeerClient {

    @Autowired
    private HugeConfig config;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public boolean configured() {
        String url = this.config.get(HubbleOptions.VERMEER_URL);
        return url != null && !url.isBlank();
    }

    public Map<String, Object> request(String method, String path,
                                      Map<String, ?> body) {
        E.checkState(this.configured(), "Configure vermeer.url first");
        URI base = URI.create(this.config.get(HubbleOptions.VERMEER_URL));
        E.checkArgument(("http".equals(base.getScheme()) ||
                         "https".equals(base.getScheme())) &&
                        base.getHost() != null && base.getUserInfo() == null &&
                        base.getQuery() == null && base.getFragment() == null,
                        "Invalid vermeer.url");
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(
                    URI.create(base.toString().replaceAll("/+$", "") +
                               "/api/v1/" + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json");
            String tokenFile = this.config.get(HubbleOptions.VERMEER_TOKEN_FILE);
            if (tokenFile != null && !tokenFile.isBlank()) {
                String token = Files.readString(Path.of(tokenFile)).trim();
                E.checkState(!token.isEmpty(), "Vermeer token file is empty");
                request.header("Authorization", "Bearer " + token);
            }
            request.method(method, body == null ?
                    HttpRequest.BodyPublishers.noBody() :
                    HttpRequest.BodyPublishers.ofString(
                            this.mapper.writeValueAsString(body)));
            HttpResponse<String> response = this.http.send(request.build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ExternalException("Vermeer request failed (HTTP " +
                                            response.statusCode() + ")");
            }
            Map<String, Object> result = this.mapper.readValue(response.body(),
                    new TypeReference<Map<String, Object>>() { });
            Object code = result.get("errcode");
            if (code != null && !"0".equals(code.toString())) {
                throw new ExternalException("Vermeer request failed");
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalException("Vermeer request interrupted", e);
        } catch (IOException e) {
            throw new ExternalException("Vermeer service is unavailable", e);
        }
    }
}
