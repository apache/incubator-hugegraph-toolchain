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

package org.apache.hugegraph.base;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Printer {

    public static void print(String content) {
        System.out.println(content);
    }

    public static void print(String content, Object ... objects) {
        System.out.println(String.format(content, objects));
    }

    public static void printKV(String key, Object value) {
        System.out.println(key + ": " + value);
    }

    public static void printSet(String name, Set<?> set) {
        System.out.println(name + ": (");
        for (Object object : set) {
            System.out.println("\t" + object + ",");
        }
        System.out.println(")");
    }

    public static void printList(String name, List<?> list) {
        System.out.println(name + ": [");
        for (Object object : list) {
            System.out.println("\t" + object + ",");
        }
        System.out.println("]");
    }

    public static void printMap(String name, Map<?, ?> map) {
        System.out.println(name + ": {");
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            System.out.println("\t" + entry.getKey() + ": " +
                               entry.getValue() + ",");
        }
        System.out.println("}");
    }

    public static void printInBackward(String message) {
        System.out.print(message);
        printInBackward(0L);
    }

    public static void printInBackward(long count) {
        System.out.print(String.format("%d%s", count, backward(count)));
    }

    public static String backward(long word) {
        StringBuilder backward = new StringBuilder();
        for (int i = 0, len = String.valueOf(word).length(); i < len; i++) {
            backward.append("\b");
        }
        return backward.toString();
    }
}
