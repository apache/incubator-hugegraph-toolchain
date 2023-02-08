/*
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

package org.apache.hugegraph.loader.executor;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.SchemaManager;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.DelegatingScript;

public class GroovyExecutor {

    private final Binding binding;

    public GroovyExecutor() {
        this.binding = new Binding();
    }

    public void bind(String name, Object value) {
        this.binding.setVariable(name, value);
    }

    public void execute(String groovyScript, HugeClient client) {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass(DelegatingScript.class.getName());
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addImports(HugeClient.class.getName());
        importCustomizer.addImports(SchemaManager.class.getName());
        config.addCompilationCustomizers(importCustomizer);

        GroovyShell shell = new GroovyShell(getClass().getClassLoader(),
                                            this.binding, config);

        // Groovy invoke java through the delegating script.
        DelegatingScript script = (DelegatingScript) shell.parse(groovyScript);
        script.setDelegate(client);
        script.run();
    }
}
