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

package org.apache.hugegraph.service.schema;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;

import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.structure.SchemaElement;
import org.apache.hugegraph.structure.constant.AggregateType;
import org.apache.hugegraph.structure.constant.Cardinality;
import org.apache.hugegraph.structure.constant.DataType;
import org.apache.hugegraph.structure.constant.Frequency;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.structure.constant.IndexType;
import org.apache.hugegraph.structure.constant.WriteType;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.SchemaLabel;
import org.apache.hugegraph.structure.schema.VertexLabel;

/**
 * Keeps Hubble Schema export compatible with Servers which return structured
 * Schema JSON for the historical {@code format=groovy} request.
 */
public final class GroovySchemaCompatibility {

    private static final String PREFIX = "graph.schema()";

    private GroovySchemaCompatibility() {
    }

    public static String export(SchemaManager schema) {
        String legacy = schema.getGroovySchema();
        if (StringUtils.isNotEmpty(legacy)) {
            return legacy;
        }

        StringBuilder result = new StringBuilder();
        schema.getPropertyKeys().stream().sorted(GroovySchemaCompatibility::byName)
              .forEach(value -> append(result, property(value)));
        schema.getVertexLabels().stream().sorted(GroovySchemaCompatibility::byName)
              .forEach(value -> append(result, vertex(value)));
        schema.getEdgeLabels().stream().sorted(GroovySchemaCompatibility::byName)
              .forEach(value -> append(result, edge(value)));
        schema.getIndexLabels().stream().sorted(GroovySchemaCompatibility::byName)
              .forEach(value -> append(result, index(value)));
        return result.toString();
    }

    private static void append(StringBuilder result, String line) {
        if (result.length() > 0) {
            result.append('\n');
        }
        result.append(line);
    }

    private static String property(PropertyKey value) {
        StringBuilder line = builder("propertyKey", value.name());
        line.append(dataType(value.dataType()));
        if (value.cardinality() == Cardinality.LIST) {
            line.append(".valueList()");
        } else if (value.cardinality() == Cardinality.SET) {
            line.append(".valueSet()");
        }
        if (value.aggregateType() != null &&
            value.aggregateType() != AggregateType.NONE) {
            line.append(".calc")
                .append(capitalize(value.aggregateType().string())).append("()");
        }
        if (value.writeType() != null && value.writeType() != WriteType.OLTP) {
            line.append(".writeType(").append(quote(value.writeType().name()))
                .append(")");
        }
        appendUserdata(line, value);
        return finish(line);
    }

    private static String vertex(VertexLabel value) {
        StringBuilder line = builder("vertexLabel", value.name());
        IdStrategy strategy = value.idStrategy();
        if (strategy == IdStrategy.AUTOMATIC) {
            line.append(".useAutomaticId()");
        } else if (strategy == IdStrategy.PRIMARY_KEY) {
            line.append(".usePrimaryKeyId()");
        } else if (strategy == IdStrategy.CUSTOMIZE_STRING) {
            line.append(".useCustomizeStringId()");
        } else if (strategy == IdStrategy.CUSTOMIZE_NUMBER) {
            line.append(".useCustomizeNumberId()");
        } else if (strategy == IdStrategy.CUSTOMIZE_UUID) {
            line.append(".useCustomizeUuidId()");
        }
        appendStrings(line, "properties", value.properties());
        appendStrings(line, "primaryKeys", value.primaryKeys());
        appendStrings(line, "nullableKeys", value.nullableKeys());
        appendTtl(line, value.ttl(), value.ttlStartTime());
        appendLabelIndex(line, value);
        appendUserdata(line, value);
        return finish(line);
    }

    private static String edge(EdgeLabel value) {
        StringBuilder line = builder("edgeLabel", value.name());
        if (value.parent()) {
            line.append(".asBase()");
        } else if (value.sub()) {
            line.append(".withBase(").append(quote(value.parentLabel())).append(")");
        } else {
            if (value.general()) {
                line.append(".asGeneral()");
            }
            value.links().forEach(link -> link.forEach((source, target) ->
                    line.append(".link(").append(quote(source)).append(", ")
                        .append(quote(target)).append(")")));
        }
        appendStrings(line, "properties", value.properties());
        appendStrings(line, "sortKeys", value.sortKeys());
        appendStrings(line, "nullableKeys", value.nullableKeys());
        if (value.frequency() == Frequency.SINGLE) {
            line.append(".singleTime()");
        } else if (value.frequency() == Frequency.MULTIPLE) {
            line.append(".multiTimes()");
        }
        appendTtl(line, value.ttl(), value.ttlStartTime());
        appendLabelIndex(line, value);
        appendUserdata(line, value);
        return finish(line);
    }

    private static String index(IndexLabel value) {
        StringBuilder line = builder("indexLabel", value.name());
        if (value.baseType() == HugeType.VERTEX_LABEL) {
            line.append(".onV(").append(quote(value.baseValue())).append(")");
        } else {
            line.append(".onE(").append(quote(value.baseValue())).append(")");
        }
        appendStrings(line, "by", value.indexFields());
        IndexType type = value.indexType();
        if (type != null) {
            line.append('.').append(type.string()).append("()");
        }
        if (!value.rebuild()) {
            line.append(".rebuild(false)");
        }
        appendUserdata(line, value);
        return finish(line);
    }

    private static StringBuilder builder(String type, String name) {
        return new StringBuilder(PREFIX).append('.').append(type).append('(')
                                        .append(quote(name)).append(')');
    }

    private static String finish(StringBuilder line) {
        return line.append(".ifNotExist().create();").toString();
    }

    private static void appendStrings(StringBuilder line, String method,
                                      Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        StringJoiner args = new StringJoiner(", ");
        values.forEach(value -> args.add(quote(value)));
        line.append('.').append(method).append('(').append(args).append(')');
    }

    private static void appendTtl(StringBuilder line, long ttl,
                                  String ttlStartTime) {
        if (ttl > 0L) {
            line.append(".ttl(").append(ttl).append(')');
        }
        if (StringUtils.isNotEmpty(ttlStartTime)) {
            line.append(".ttlStartTime(").append(quote(ttlStartTime)).append(')');
        }
    }

    private static void appendLabelIndex(StringBuilder line,
                                         SchemaLabel value) {
        try {
            line.append(".enableLabelIndex(")
                .append(value.enableLabelIndex()).append(')');
        } catch (NullPointerException ignored) {
            // Older Server responses may omit the nullable option; builder
            // default is the only faithful representation in that case.
        }
    }

    private static void appendUserdata(StringBuilder line,
                                       SchemaElement value) {
        value.userdata().entrySet().stream()
             .sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                 line.append(".userdata(").append(quote(entry.getKey()))
                     .append(", ").append(literal(entry.getValue())).append(')');
             });
    }

    private static String dataType(DataType type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case TEXT: return ".asText()";
            case INT: return ".asInt()";
            case DATE: return ".asDate()";
            case UUID: return ".asUUID()";
            case BOOLEAN: return ".asBoolean()";
            case BYTE: return ".asByte()";
            case BLOB: return ".asBlob()";
            case DOUBLE: return ".asDouble()";
            case FLOAT: return ".asFloat()";
            case LONG: return ".asLong()";
            default:
                return ".dataType(org.apache.hugegraph.structure.constant." +
                       "DataType." + type.name() + ")";
        }
    }

    private static String literal(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Collection<?>) {
            StringJoiner values = new StringJoiner(", ", "[", "]");
            ((Collection<?>) value).forEach(item -> values.add(literal(item)));
            return values.toString();
        }
        if (value instanceof Map<?, ?>) {
            if (((Map<?, ?>) value).isEmpty()) {
                return "[:]";
            }
            StringJoiner values = new StringJoiner(", ", "[", "]");
            ((Map<?, ?>) value).forEach((key, item) ->
                    values.add(quote(String.valueOf(key)) + ": " +
                               literal(item)));
            return values.toString();
        }
        if (value.getClass().isArray()) {
            StringJoiner values = new StringJoiner(", ", "[", "]");
            for (int index = 0; index < Array.getLength(value); index++) {
                values.add(literal(Array.get(value, index)));
            }
            return values.toString();
        }
        if (value instanceof CharSequence || value instanceof Character ||
            value instanceof Enum<?>) {
            return quote(value.toString());
        }
        throw new IllegalArgumentException("Unsupported userdata type: " +
                                           value.getClass().getName());
    }

    public static List<String> splitStatements(String content) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean escaped = false;
        for (int index = 0; index < content.length(); index++) {
            char value = content.charAt(index);
            if (quote != 0) {
                current.append(value);
                if (escaped) {
                    escaped = false;
                } else if (value == '\\') {
                    escaped = true;
                } else if (value == quote) {
                    quote = 0;
                }
                continue;
            }
            if (value == '\'' || value == '"') {
                quote = value;
                current.append(value);
            } else if (value == ';' || value == '\n' || value == '\r') {
                addStatement(statements, current);
            } else {
                current.append(value);
            }
        }
        if (quote != 0 || escaped) {
            throw new IllegalArgumentException("Unterminated Groovy string");
        }
        addStatement(statements, current);
        return statements;
    }

    private static void addStatement(List<String> statements,
                                     StringBuilder current) {
        if (current.length() > 0) {
            statements.add(current.toString());
            current.setLength(0);
        }
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        return "'" + value.replace("\\", "\\\\")
                            .replace("'", "\\'")
                            .replace("\r", "\\r")
                            .replace("\n", "\\n") + "'";
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static int byName(SchemaElement left, SchemaElement right) {
        return left.name().compareTo(right.name());
    }
}
