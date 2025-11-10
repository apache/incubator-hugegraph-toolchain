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

package org.apache.hugegraph.handler;

import java.util.List;

public class ErrorCodeMessage {
    public static String getErrorMessage(String code, String message,
                                         Object... args) {
        // 1: server 2:client
        if (code.startsWith("1")) {
            return ServerCode.fromCode(code, message, args);
        } else if (code.startsWith("2")) {
            return ClientCode.fromCode(code, message, args);
        } else {
            return String.format("无效的错误码: %s，错误信息：%s", code, message);
        }
    }

    public enum ClientCode {

        // 00-xxx for common error
        SUCCEED("200-000", "执行成功"),

        FAILED("200-001", "%s"),

        NULL_VALUE("200-002", "参数 %s 不能为 null"),

        INVALID_VALUE("200-003", "%s！"),

        MISSING_PARAM("200-004", "缺少参数 %s"),

        NOT_EXISTS("200-005", "参数 %s 不存在！"),

        EXISTS("200-006", "%s 已存在！"),

        INVALID_PARAM("200-007", "%s（%s）取值非法!"),

        INVALID_VALUE_OUT_RANGE("200-008",
                                "参数越界：参数 %s 的取值范围为 %s."),

        // 01-xxx for graphSpace error
        // 02-xxx for graphs error
        // 03-xxx for schema error
        UNDEFINED_PROPERTY_KEY("203-000", "不存在的属性类型：%s"),

        UNDEFINED_VERTEX_LABEL("203-001", "不存在的顶点类型：%s"),

        UNDEFINED_EDGE_LABEL("203-002", "不存在的边类型：%s"),
        // 04-xxx for vertex and edge error
        // 05-xxx for oltp algorithms error

        /*
         * param out of range error
         * param name: degree, capacity, limit
         * */
        PARAM_OUT_RANGE("205-000", "%s 取值范围 > 0 或 == %s，当前值：%s"),

        PARAM_GREATER("205-001", "%s 必须 >= %s，当前值为 '%s' 和 '%s'"),

        PARAM_SMALLER("205-002", "%s 必须 < %s"),
        ;

        private String code;
        private String message;
        private List<Object> attach;

        ClientCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public static String fromCode(String code, String message,
                                      Object... args) {
            for (ClientCode cc: ClientCode.values()) {
                if (cc.code.equals(code)) {
                    return cc.getMessage(args);
                }
            }
            return String.format("无效的错误码: %s，错误信息：%s", code, message);
        }

        public String getMessage(Object... args) {
            return String.format(this.message, args);
        }
    }

    public enum ServerCode {

        // 00-xxx for common error
        SUCCEED("100-000", "执行成功"),

        FAILED("100-001", "%s"),

        NULL_VALUE("100-002", "参数 %s 不能为 null"),

        INVALID_VALUE("100-003", "%s！"),

        MISSING_PARAM("100-004", "缺少参数 %s"),

        NOT_EXISTS("100-005", "参数 %s 不存在！"),

        EXISTS("100-006", "%s 已存在！"),

        INVALID_PARAM("100-007", "%s（%s）取值非法!"),

        INVALID_VALUE_OUT_RANGE("100-008",
                                "参数越界：参数 %s 的取值范围为 %s."),
        // 01-xxx for graphSpace error
        // 02-xxx for graphs error
        // 03-xxx for schema error
        UNDEFINED_PROPERTY_KEY("103-000", "不存在的属性类型：%s"),

        UNDEFINED_VERTEX_LABEL("103-001", "不存在的顶点类型 %s"),

        UNDEFINED_EDGE_LABEL("103-002", "不存在的边类型：%s"),

        INVALID_PROP_VALUE("103-003", "属性值 %s 的数据类型错误, " +
                                      "需要的类型(字段)为 %s(%s), 实际类型 %s"),

        // 04-xxx for vertex and edge error
        VERTEX_NOT_EXIST("104-001", "顶点 %s 不存在"),

        VERTEX_NAME_NOT_EXIST("104-002", "参数 %s 的值 '%s' 不存在"),

        IDS_NOT_EXIST("104-003", "不存在的 ids %s"),

        EDGE_INVALID_LINK("104-004", "不存在的 source/target 类型 %s，%s 对应边类型 %s"),

        LABEL_PROP_NOT_EXIST("104-005", "不存在顶点类型为 %s，属性为 %s 的顶点"),

        // 05-xxx for oltp algorithms error
        REACH_CAPACITY("105-003", "达到临界 capacity %s"),

        REACH_CAPACITY_WITH_DEPTH("105-004", "达到临界 capacity %s，" +
                                             "剩余 depth 为 %s"),

        EXCEED_CAPACITY_WHILE_FINDING("105-005", "达到临界 capacity %s，" +
                                                 "查找对象：%s"),

        INVALID_LIMIT("105-006", "无效的 limit %s, 必须 <= capacity(%s)"),

        PARAM_GREATER("105-007", "%s 必须 >= %s, 当前值为 %s 和 %s"),

        PARAM_SMALLER("105-008", "%s 必须 < %s"),

        DEPTH_OUT_RANGE("105-009", "depth 取值范围 (0, 5000], " +
                                   "当前值: %s"),

        VERTEX_PAIR_LENGTH_ERROR("105-010", "顶点对长度错误"),

        SOURCE_TARGET_SAME("105-011", "source 和 target 不能取相同 id"),
        ;

        private String code;
        private String message;

        ServerCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public static String fromCode(String code, String message,
                                      Object... args) {
            for (ServerCode sc: ServerCode.values()) {
                if (sc.code.equals(code)) {
                    return sc.getMessage(args);
                }
            }
            return String.format("无效的错误码: %s，错误信息：%s", code, message);
        }

        public String getMessage(Object... args) {
            return String.format(this.message, args);
        }
    }
}
