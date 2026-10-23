/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.handler;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.common.Response;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.rest.SerializeException;
import org.apache.hugegraph.service.op.OperationsNodeNotFoundException;
import org.apache.hugegraph.testutil.Assert;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class ExceptionAdvisorStatusTest {

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void testQueryDeserializeErrorsDoNotReturnTheOriginalLargeResult() {
        String payload = "x".repeat(1024 * 1024) + "error-body-tail-canary";
        for (String route : new String[]{"gremlin-query", "cypher"}) {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                    new MockHttpServletRequest("POST", "/api/v1.3/graphspaces/DEFAULT/graphs/g/" + route)));
            SerializeException failure = new SerializeException("Failed to deserialize: %s",
                                         new IllegalArgumentException("name length exceeded"), payload);
            Response response = advisor().exceptionHandler((Exception) failure);
            Assert.assertEquals(Constant.STATUS_BAD_REQUEST, response.getStatus());
            Assert.assertTrue(response.getMessage().startsWith("Failed to deserialize:"));
            Assert.assertTrue(response.getMessage().length() <= 2048);
            Assert.assertTrue(response.getMessage().endsWith("[message truncated]"));
            Assert.assertFalse(response.getMessage().contains("error-body-tail-canary"));
            Assert.assertNull(response.getCause());
        }
    }

    @Test
    public void testQueryMessageLimitDoesNotBreakSecretRedaction() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/api/v1.3/graphspaces/DEFAULT/graphs/g/gremlin-query")));
        String secret = "private-canary".repeat(10000);
        for (String message : new String[]{"Failed to deserialize: {\"password\":\"" + secret + "\"}",
                                           "Failed to deserialize: -----BEGIN RSA PRIVATE KEY-----\n" +
                                           secret + "\n-----END RSA PRIVATE KEY-----"}) {
            Response response = advisor().exceptionHandler((Exception) new IllegalArgumentException(message));
            Assert.assertFalse(response.getMessage().contains("private-canary"));
            Assert.assertTrue(response.getMessage().contains("[REDACTED]"));
            Assert.assertTrue(response.getMessage().length() <= 2048);
        }
    }

    @Test
    public void testQueryErrorArgumentsAreFormattedBeforeTheFinalLimit() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/api/v1.3/graphspaces/DEFAULT/graphs/g/gremlin-query")));
        MessageSourceHandler messages = Mockito.mock(MessageSourceHandler.class);
        Mockito.when(messages.getMessage(Mockito.anyString(), Mockito.any(String[].class)))
               .thenAnswer(invocation -> {
                   Object[] args = (Object[]) invocation.getRawArguments()[1];
                   String argument = args[0].toString();
                   Assert.assertTrue(argument.length() > 2048);
                   Assert.assertFalse(argument.contains("[message truncated]"));
                   return "Query failed: " + argument;
               });
        ExceptionAdvisor advisor = new ExceptionAdvisor();
        ReflectionTestUtils.setField(advisor, "messageSourceHandler", messages);
        Response response = advisor.exceptionHandler(new ExternalException(503, "gremlin.execute.failed",
                                                      "detail " + "x".repeat(1024 * 1024)));
        Assert.assertEquals(503, response.getStatus());
        Assert.assertTrue(response.getMessage().startsWith("Query failed: detail"));
        Assert.assertTrue(response.getMessage().length() <= 2048);
        Assert.assertTrue(response.getMessage().endsWith("[message truncated]"));
    }

    @Test
    public void testLongStructuredQueryErrorResolvesItsCodeBeforeTheFinalLimit() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/api/v1.3/graphspaces/DEFAULT/graphs/g/gremlin-query")));
        String payload = "x".repeat(1024 * 1024);
        String structured = "{\"message\":\"" + payload + "\",\"code\":\"104-001\"," +
                            "\"attach\":[\"vertex-name\"]}";
        ServerException failure = new ServerException(structured);
        failure.status(HttpStatus.FORBIDDEN.value());

        Response response = advisor().exceptionHandler(failure);

        Assert.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        Assert.assertEquals(ErrorCodeMessage.getErrorMessage("104-001", payload, "vertex-name"),
                            response.getMessage());
        Assert.assertFalse(response.getMessage().contains("[message truncated]"));
        Assert.assertNull(response.getCause());

        String longArgument = "vertex-name " + payload;
        structured = "{\"code\":\"104-001\",\"message\":\"upstream failure\"," +
                     "\"attach\":[\"" + longArgument + "\"]}";
        Response bounded = advisor().exceptionHandler(new ExternalException(503, structured));

        Assert.assertEquals(503, bounded.getStatus());
        Assert.assertTrue(bounded.getMessage().startsWith("顶点 vertex-name "));
        Assert.assertTrue(bounded.getMessage().length() <= 2048);
        Assert.assertTrue(bounded.getMessage().endsWith("[message truncated]"));
    }

    @Test
    public void testLongQueryMessageIsRedactedBeforeLocalizationDiagnostics() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/api/v1.3/graphspaces/DEFAULT/graphs/g/gremlin-query")));
        MessageSourceHandler messages = Mockito.mock(MessageSourceHandler.class);
        Mockito.when(messages.getMessage(Mockito.anyString(), Mockito.nullable(Object[].class)))
               .thenAnswer(invocation -> {
                   String message = invocation.getArgument(0);
                   Assert.assertFalse(message.contains("private-canary"));
                   return message;
               });
        ExceptionAdvisor advisor = new ExceptionAdvisor();
        ReflectionTestUtils.setField(advisor, "messageSourceHandler", messages);
        String message = "Failed to deserialize: {\"password\":\"" +
                         "private-canary".repeat(10000) + "\"}";

        Response response = advisor.exceptionHandler(new ExternalException(503, message));

        Assert.assertEquals(503, response.getStatus());
        Assert.assertTrue(response.getMessage().contains("[REDACTED]"));
        Assert.assertTrue(response.getMessage().length() <= 2048);
    }

    @Test
    public void testQueryShortErrorsAndOtherApiMessagesAreUnchanged() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/api/v1.3/graphspaces/DEFAULT/graphs/g/gremlin-query")));
        Assert.assertEquals("syntax error", advisor().exceptionHandler(
                            (Exception) new IllegalArgumentException("syntax error")).getMessage());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("GET", "/api/v1.3/graphs")));
        String message = "x".repeat(8192);
        Assert.assertEquals(message, advisor().exceptionHandler(
                            (Exception) new IllegalArgumentException(message)).getMessage());
    }

    @Test
    public void testPreserveServerAuthenticationAndPermissionStatus() {
        Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(),
                            ExceptionAdvisor.serverStatus(401));
        Assert.assertEquals(HttpStatus.FORBIDDEN.value(),
                            ExceptionAdvisor.serverStatus(403));
        Assert.assertEquals(Constant.STATUS_BAD_REQUEST,
                            ExceptionAdvisor.serverStatus(500));
    }

    @Test
    public void testOperationsMissingNodeIsAQuietNotFound() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest()));

        Response response = new ExceptionAdvisor().exceptionHandler(
                            new OperationsNodeNotFoundException());

        Assert.assertEquals(HttpStatus.NOT_FOUND.value(),
                            response.getStatus());
        Assert.assertEquals("operations_node_not_found",
                            response.getMessage());
        Assert.assertNull(response.getCause());
    }

    @Test
    public void testServerConnectionFailureDoesNotExposeAddress() {
        operationsRequest();
        ServerException failure = new ServerException(
                "Failed to connect to /127.0.0.1:8080/private");
        failure.status(HttpStatus.INTERNAL_SERVER_ERROR.value());

        Response response = advisor().exceptionHandler(failure);

        Assert.assertEquals(Constant.STATUS_BAD_REQUEST,
                            response.getStatus());
        Assert.assertEquals("upstream_unavailable", response.getMessage());
        Assert.assertFalse(response.getMessage().contains("127.0.0.1"));
        Assert.assertFalse(response.getMessage().contains("8080"));
        Assert.assertFalse(response.getMessage().contains("private"));
    }

    @Test
    public void testServerAuthenticationFailureKeepsStatusWithoutBody() {
        operationsRequest();
        ServerException failure = new ServerException(
                                  "Authentication failed secret-canary");
        failure.status(HttpStatus.UNAUTHORIZED.value());

        Response response = advisor().exceptionHandler(failure);

        Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(),
                            response.getStatus());
        Assert.assertEquals("upstream_unauthorized", response.getMessage());
        Assert.assertFalse(response.getMessage().contains("secret-canary"));
    }

    @Test
    public void testServerPermissionFailureKeepsStatusWithoutBody() {
        operationsRequest();
        ServerException failure = new ServerException(
                                  "Forbidden http://user:secret@host/private");
        failure.status(HttpStatus.FORBIDDEN.value());

        Response response = advisor().exceptionHandler(failure);

        Assert.assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        Assert.assertEquals("upstream_forbidden", response.getMessage());
        Assert.assertFalse(response.getMessage().contains("secret"));
        Assert.assertFalse(response.getMessage().contains("private"));
    }

    @Test
    public void testUnexpectedFailureUsesStableSafeMessage() {
        operationsRequest();
        RuntimeException failure = new RuntimeException(
                "http://user:secret@127.0.0.1:8080/private secret-canary");

        Response response = advisor().exceptionHandler(failure);

        Assert.assertEquals(Constant.STATUS_BAD_REQUEST, response.getStatus());
        Assert.assertEquals("unexpected_request_failure",
                            response.getMessage());
        Assert.assertFalse(response.getMessage().contains("127.0.0.1"));
        Assert.assertFalse(response.getMessage().contains("secret-canary"));
    }

    @Test
    public void testNonOperationsBusinessErrorPreservesSafeMessage() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("GET", "/api/v1.3/graphs")));
        ServerException failure = new ServerException("graph_not_found");
        failure.status(HttpStatus.NOT_FOUND.value());

        Response response = advisor().exceptionHandler(failure);

        Assert.assertEquals("graph_not_found", response.getMessage());
    }

    @Test
    public void testDiagnosticStackTraceRedactsCredentialsAndEndpoints() {
        RuntimeException failure = new RuntimeException(
                "http://user:secret@host/private?token=abc&password=xyz&" +
                "endpoint=10.0.0.1:8080");

        String diagnostic = ExceptionAdvisor.sanitize(failure.toString());

        Assert.assertFalse(diagnostic.contains("secret"));
        Assert.assertFalse(diagnostic.contains("abc"));
        Assert.assertFalse(diagnostic.contains("xyz"));
        Assert.assertFalse(diagnostic.contains("10.0.0.1"));
        Assert.assertTrue(diagnostic.contains("[REDACTED]"));
    }

    @Test
    public void testDiagnosticRedactsNestedStructuredSecrets() {
        RuntimeException failure = new RuntimeException(
                "Authorization: Basic YmFzaWMtY2FuYXJ5 " +
                "{\"apiKey\":\"json-canary\"," +
                "\"secretKey\":\"secret-key-canary\"," +
                "\"client_secret\":\"client-secret-canary\"," +
                "\"secret\":\"secret-canary\"}",
                new IllegalStateException(
                        "token=token-canary credential=credential-canary"));
        failure.addSuppressed(new IllegalArgumentException(
                "password: password-canary, api_key=key-canary, " +
                "endpoint: endpoint-canary"));

        String diagnostic = ExceptionAdvisor.sanitize(
                            failure + " " + failure.getCause() + " " +
                            failure.getSuppressed()[0]);

        Assert.assertFalse(diagnostic.contains("YmFzaWMtY2FuYXJ5"));
        Assert.assertFalse(diagnostic.contains("json-canary"));
        Assert.assertFalse(diagnostic.contains("secret-canary"));
        Assert.assertFalse(diagnostic.contains("secret-key-canary"));
        Assert.assertFalse(diagnostic.contains("client-secret-canary"));
        Assert.assertFalse(diagnostic.contains("token-canary"));
        Assert.assertFalse(diagnostic.contains("credential-canary"));
        Assert.assertFalse(diagnostic.contains("password-canary"));
        Assert.assertFalse(diagnostic.contains("key-canary"));
        Assert.assertFalse(diagnostic.contains("endpoint-canary"));
        Assert.assertTrue(diagnostic.contains("[REDACTED]"));
    }

    @Test
    public void testDiagnosticRedactsHeadersPrivateKeysAndAbsolutePaths() {
        RuntimeException failure = new RuntimeException(
                "Cookie: session=cookie-canary\n" +
                "Set-Cookie: auth=set-cookie-canary; HttpOnly\n" +
                "-----BEGIN PRIVATE KEY-----\nprivate-key-canary\n" +
                "-----END PRIVATE KEY-----\n" +
                "failed at /Users/operator/private/config.yaml and " +
                "C:\\Users\\operator\\private\\config.yaml");

        String diagnostic = ExceptionAdvisor.sanitize(failure.getMessage());

        Assert.assertFalse(diagnostic.contains("cookie-canary"));
        Assert.assertFalse(diagnostic.contains("set-cookie-canary"));
        Assert.assertFalse(diagnostic.contains("private-key-canary"));
        Assert.assertFalse(diagnostic.contains("/Users/operator"));
        Assert.assertFalse(diagnostic.contains("C:\\Users\\operator"));
        Assert.assertTrue(diagnostic.contains("[REDACTED]"));
    }

    @Test
    public void testNonOperationsResponseRedactsStructuredSecrets() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("GET", "/api/v1.3/graphs")));
        ServerException failure = new ServerException(
                "request failed {\"token\":\"json-token-canary\"} " +
                "Cookie: session=cookie-canary\n" +
                "at /Users/operator/private/config.yaml");
        failure.status(HttpStatus.INTERNAL_SERVER_ERROR.value());

        Response response = advisor().exceptionHandler(failure);

        Assert.assertFalse(response.getMessage().contains("json-token-canary"));
        Assert.assertFalse(response.getMessage().contains("cookie-canary"));
        Assert.assertFalse(response.getMessage().contains("/Users/operator"));
    }

    private static void operationsRequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("GET", "/api/v1.3/operations/nodes")));
    }

    private static ExceptionAdvisor advisor() {
        MessageSourceHandler messages = Mockito.mock(
                                        MessageSourceHandler.class);
        Mockito.when(messages.getMessage(Mockito.anyString(),
                                          Mockito.nullable(Object[].class)))
               .thenAnswer(invocation -> invocation.getArgument(0));
        ExceptionAdvisor advisor = new ExceptionAdvisor();
        ReflectionTestUtils.setField(advisor, "messageSourceHandler", messages);
        return advisor;
    }
}
