/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.hugegraph.config.ConfigOption;
import org.apache.hugegraph.config.HugeConfig;

public class LangChainControllerSecurityTest {

    @Test
    public void testRequestPythonPathIsIgnored() throws Exception {
        LangChainController controller = new LangChainController();
        String[] args = this.executeArgs(controller, "/tmp/evil-python",
                                        "/tmp/script.py");

        Assert.assertNotEquals("/tmp/evil-python", args[0]);
    }

    @Test
    public void testNoSchemaRejectsPathTraversalFileName() {
        LangChainController controller = new LangChainController();
        LangChainController.RequestLangChainParams params =
                LangChainController.RequestLangChainParams.builder()
                                                          .query("show vertices")
                                                          .model("gpt4")
                                                          .openKey("key")
                                                          .pythonPath("/bin/echo")
                                                          .fileName("../../pom.xml")
                                                          .build();

        assertThrows(IllegalArgumentException.class, () -> {
            controller.langchainNoSchema("DEFAULT", "graph", params);
        });
    }

    @Test
    public void testNoSchemaRejectsNonAllowlistedScript() throws Exception {
        LangChainController controller = this.controllerWithProcessConfig(5);
        LangChainController.RequestLangChainParams params =
                LangChainController.RequestLangChainParams.builder()
                                                          .query("show vertices")
                                                          .model("gpt4")
                                                          .openKey("key")
                                                          .pythonPath("/bin/echo")
                                                          .fileName("other.py")
                                                          .build();

        assertThrows(IllegalArgumentException.class, () -> {
            controller.langchainNoSchema("DEFAULT", "graph", params);
        });
    }

    @Test
    public void testNoSchemaUsesConfiguredPythonForAllowedScript()
           throws Exception {
        LangChainController controller = this.controllerWithProcessConfig(5);
        LangChainController.RequestLangChainParams params =
                LangChainController.RequestLangChainParams.builder()
                                                          .query("show vertices")
                                                          .model("gpt4")
                                                          .openKey("key")
                                                          .pythonPath("/bin/echo")
                                                          .fileName("langchaincode/excute_langchain.py")
                                                          .build();

        LangChainController.ResponseLangChain response =
                (LangChainController.ResponseLangChain) controller.langchainNoSchema(
                        "DEFAULT", "graph", params);

        Assert.assertEquals("g.V()", response.getGremlin());
    }

    @Test
    public void testProcessNonZeroExitFails() throws Exception {
        LangChainController controller = this.controllerWithProcessConfig(5);

        assertThrows(IllegalStateException.class, () -> {
            this.executeByProcessBuilder(controller, "/bin/sh",
                                         this.resourcePath("langchaincode/fail.py"));
        });
    }

    @Test
    public void testProcessTimeoutFails() throws Exception {
        LangChainController controller = this.controllerWithProcessConfig(1);
        long start = System.nanoTime();

        assertThrows(IllegalStateException.class, () -> {
            this.executeByProcessBuilder(controller, "/bin/sh",
                                         this.resourcePath("langchaincode/sleep.py"));
        });

        long elapsed = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
        Assert.assertTrue(elapsed < 5L);
    }

    @Test
    public void testSecretValuesAreRedactedFromProcessOutput()
           throws Exception {
        LangChainController controller = new LangChainController();
        Set<String> secrets = new HashSet<>();
        secrets.add("open-secret");
        secrets.add("ernie-secret");

        String redacted = this.redact(controller,
                                      "argv=open-secret err=ernie-secret",
                                      secrets);

        Assert.assertEquals("argv=****** err=******", redacted);
    }

    @Test
    public void testNoGremlinFailureReturnsEmptyResult()
           throws Exception {
        LangChainController controller = this.controllerWithProcessConfig(5);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(output, true, StandardCharsets.UTF_8));
        List<String> result;
        try {
            result = this.executeByProcessBuilder(
                    controller, "/bin/sh",
                    this.resourcePath("langchaincode/secret_no_gremlin.py"),
                    "open-secret");
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }

        Assert.assertTrue(result.isEmpty());
        String logs = output.toString(StandardCharsets.UTF_8);
        if (!logs.isEmpty()) {
            Assert.assertFalse(logs.contains("open-secret"));
            Assert.assertTrue(logs.contains("******"));
        }
    }

    private String[] executeArgs(LangChainController controller, String python,
                                 String script) throws Exception {
        Method method = LangChainController.class.getDeclaredMethod(
                        "getExcuteArgs", String.class, String.class,
                        String.class, String.class, String.class,
                        String.class, String.class, String.class);
        method.setAccessible(true);
        return (String[]) method.invoke(controller, python, script, "q", "open",
                                        "schema", "gpt4", null, null);
    }

    private List<String> executeByProcessBuilder(LangChainController controller,
                                                 String python, String script)
                                                 throws Exception {
        return this.executeByProcessBuilder(controller, python, script, "open");
    }

    @SuppressWarnings("unchecked")
    private List<String> executeByProcessBuilder(LangChainController controller,
                                                 String python, String script,
                                                 String openKey)
                                                 throws Exception {
        Method method = LangChainController.class.getDeclaredMethod(
                        "excutePythonByProcessBuilder", String.class,
                        String.class, String.class, String.class, String.class,
                        String.class, String.class, String.class);
        method.setAccessible(true);
        try {
            return (List<String>) method.invoke(controller, python, script, "q",
                                                openKey, "schema", "gpt4",
                                                null, null);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }

    private LangChainController controllerWithProcessConfig(int timeout)
                                           throws Exception {
        LangChainController controller = new LangChainController();
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(Mockito.any()))
               .thenAnswer(invocation -> {
                   ConfigOption<?> option = invocation.getArgument(0);
                   if ("langchain.python_path".equals(option.name())) {
                       return "/bin/sh";
                   }
                   if ("langchain.script_dir".equals(option.name())) {
                       return this.resourcePath("langchaincode");
                   }
                   if ("langchain.script_allowlist".equals(option.name())) {
                       return Collections.singletonList("excute_langchain.py");
                   }
                   if ("langchain.execute_timeout".equals(option.name())) {
                       return timeout;
                   }
                   return option.defaultValue();
               });
        Field field = LangChainController.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(controller, config);
        return controller;
    }

    private String resourcePath(String name) {
        return LangChainControllerSecurityTest.class.getClassLoader()
                                                   .getResource(name)
                                                   .getPath();
    }

    private String redact(LangChainController controller, String value,
                          Set<String> secrets) throws Exception {
        Method method = LangChainController.class.getDeclaredMethod(
                        "redact", String.class, Set.class);
        method.setAccessible(true);
        return (String) method.invoke(controller, value, secrets);
    }

    private static void assertThrows(Class<? extends Throwable> expected,
                                     ThrowingRunnable runnable) {
        try {
            runnable.run();
            Assert.fail("Expected " + expected.getName());
        } catch (Throwable e) {
            Assert.assertTrue("Expected " + expected.getName() + " but got " +
                              e.getClass().getName(),
                              expected.isInstance(e));
        }
    }

    private interface ThrowingRunnable {

        void run() throws Throwable;
    }
}
