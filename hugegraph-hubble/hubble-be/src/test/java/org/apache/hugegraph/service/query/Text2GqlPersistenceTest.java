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

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.entity.query.Text2GqlRequest;
import org.apache.hugegraph.entity.query.Text2GqlSettingsRequest;
import org.apache.hugegraph.mapper.query.Text2GqlConfigMapper;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Text2GqlPersistenceTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void testH2SnapshotAndEncryptedKeySurviveRestartAndRemainSingleton() throws Exception {
        String url = "jdbc:h2:file:" + this.temporary.newFolder("database").toPath().resolve("metadata") +
                     ";DB_CLOSE_ON_EXIT=FALSE";
        Path keyFile = this.temporary.getRoot().toPath().resolve("master.key");
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.TEXT2GQL_SHARED_ENABLED)).thenReturn(true);
        Mockito.when(config.get(HubbleOptions.TEXT2GQL_SHARED_ALLOW_ANONYMOUS)).thenReturn(false);
        JdbcDataSource first = dataSource(url);
        new ResourceDatabasePopulator(new FileSystemResource("src/main/resources/database/schema.sql")).execute(first);
        Text2GqlSettingsRequest request = new Text2GqlSettingsRequest();
        request.setEnabled(true);
        request.setConnectionType("openai");
        request.setBaseUrl("http://127.0.0.1:11434/v1");
        request.setModel("first-model");
        request.setApiKey("secret-that-must-not-be-plaintext-in-h2");
        try (SqlSession session = sessions(first).openSession(true)) {
            org.junit.Assert.assertNull(session.getMapper(Text2GqlConfigMapper.class).current());
            Text2GqlSettingsService service = new Text2GqlSettingsService(config,
                    session.getMapper(Text2GqlConfigMapper.class),
                    new Text2GqlSecretStore(keyFile, ignored -> null), ignored -> null);
            service.save(request);
            request.setApiKey(null);
            request.setModel("second-model");
            service.save(request);
        }
        try (Connection connection = first.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("SELECT * FROM text2gql_config")) {
                assertTrue(result.next());
                assertEquals(1, result.getInt("id"));
                assertEquals("second-model", result.getString("model"));
                assertTrue(result.getString("encrypted_api_key").startsWith("v1:"));
                assertFalse(result.getString("encrypted_api_key").contains("secret-that-must-not-be-plaintext"));
                assertFalse(result.next());
            }
            statement.execute("SHUTDOWN");
        }
        JdbcDataSource restarted = dataSource(url);
        try (SqlSession session = sessions(restarted).openSession(true)) {
            Text2GqlSettingsService service = new Text2GqlSettingsService(config,
                    session.getMapper(Text2GqlConfigMapper.class),
                    new Text2GqlSecretStore(keyFile, ignored -> null), ignored -> null);
            Text2GqlRequest question = new Text2GqlRequest();
            question.setSource("shared");
            question.setQuestion("Find people");
            Text2GqlRequest resolved = service.sharedRequest(question, false);
            assertEquals("second-model", resolved.getModel());
            assertEquals("secret-that-must-not-be-plaintext-in-h2", resolved.getApiKey());
            assertEquals("managed", service.settings().get("source"));
            request.setEnabled(false);
            service.save(request);
            assertEquals(Map.of("available", false, "reason", "not_configured"), service.availability(false));
        }
        try (Connection connection = restarted.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
    }

    private static JdbcDataSource dataSource(String url) {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL(url);
        source.setUser("sa");
        source.setPassword("");
        return source;
    }

    private static SqlSessionFactory sessions(JdbcDataSource dataSource) {
        Configuration configuration = new Configuration(new Environment(
                "text2gql-test", new JdbcTransactionFactory(), dataSource));
        configuration.addMapper(Text2GqlConfigMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }
}
