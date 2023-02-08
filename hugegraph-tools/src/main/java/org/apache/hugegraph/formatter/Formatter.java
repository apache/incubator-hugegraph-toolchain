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

package org.apache.hugegraph.formatter;

import org.apache.hugegraph.structure.JsonGraph;

public interface Formatter {

    // Serialize a vertex(with edge and property) to string
    public String dump(JsonGraph.JsonVertex vertex) throws Exception;

    public static final String PACKAGE = Formatter.class.getPackage().getName();

    public static Formatter loadFormatter(String formatter) {
        String classPath = String.format("%s.%s", PACKAGE, formatter);
        ClassLoader loader = Formatter.class.getClassLoader();
        try {
            // Check subclass
            Class<?> clazz = loader.loadClass(classPath);
            if (!Formatter.class.isAssignableFrom(clazz)) {
                throw new RuntimeException("Invalid formatter: " + formatter);
            }
            // New instance of formatter
            return (Formatter) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Can't load formatter: " + formatter, e);
        }
    }
}
