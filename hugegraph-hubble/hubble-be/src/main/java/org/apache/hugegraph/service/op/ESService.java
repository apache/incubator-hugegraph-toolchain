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

package org.apache.hugegraph.service.op;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.util.E;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Objects;

@Log4j2
public abstract class ESService {
    @Autowired
    private HugeConfig config;

    public static final String[] LEVELS = new String[]{"TRACE", "OFF",
            "FATAL", "ERROR", "WARN", "INFO", "DEBUG"};

    public static volatile ElasticsearchClient elasticsearchClient;

    public synchronized ElasticsearchClient esClient() {

        if (elasticsearchClient != null) {
            return elasticsearchClient;
        }

        RestClient restClient = esRestClient();

        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());
        // And create the API client
        elasticsearchClient = new ElasticsearchClient(transport);

        return elasticsearchClient;
    }

    protected RestClient esRestClient() {
        String esURLS = null;

        // Get monitor.url from system.env
        esURLS = System.getenv(HubbleOptions.ES_URL.name());
        if (StringUtils.isEmpty(esURLS)) {
            // get monitor.url from file: hugegraph-hubble.properties
            esURLS = config.get(HubbleOptions.ES_URL);
        }

        E.checkArgument(StringUtils.isNotEmpty(esURLS),
                        "Please set \"es.urls\" in system environments " +
                                "or config file(hugegraph-hubble.properties).");

        String[] esAddresses = esURLS.split(",");
        HttpHost[] hosts = Arrays.stream(esAddresses)
                                 .map(HttpHost::create)
                                 .filter(Objects::nonNull)
                                 .toArray(HttpHost[]::new);
        log.debug("es.hosts:{}", Arrays.toString(hosts));

        RestClientBuilder restClientBuidler = RestClient.builder(hosts);

        String esUser = config.get(HubbleOptions.ES_USER);
        String esPassword = config.get(HubbleOptions.ES_PASSWORD);
        if (StringUtils.isNotEmpty(esUser)) {
            final CredentialsProvider credentialsProvider =
                    new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                                               new UsernamePasswordCredentials(
                                                       esUser,
                                                       esPassword));

            restClientBuidler.setHttpClientConfigCallback(
                    httpClientBuilder -> httpClientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider)
            );
        }

        RestClient restClient = restClientBuidler.build();

        return restClient;
    }

    protected String logAuditPattern() {
        return config.get(HubbleOptions.LOG_AUDIT_PATTERN);
    }

    protected int exportCountLimit() {
        return config.get(HubbleOptions.LOG_EXPORT_COUNT);
    }

    protected int maxResultWindow() {
        return config.get(HubbleOptions.MAX_RESULT_WINDOW);
    }
}
