/*
 * Copyright 2017 HugeGraph Authors
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

package com.baidu.hugegraph.loader.util;

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import com.baidu.hugegraph.loader.source.AbstractSource;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.source.file.ListFormat;
import com.baidu.hugegraph.structure.constant.Cardinality;
import com.baidu.hugegraph.structure.constant.DataType;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.InsertionOrderUtil;
import com.baidu.hugegraph.util.ReflectionUtil;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;

public final class DataTypeUtil {

    private static final Set<String> ACCEPTABLE_TRUE = ImmutableSet.of(
            "true", "1", "yes", "y"
    );
    private static final Set<String> ACCEPTABLE_FALSE = ImmutableSet.of(
            "false", "0", "no", "n"
    );

    public static boolean isSimpleValue(Object value) {
        if (value == null) {
            return false;
        }
        return ReflectionUtil.isSimpleType(value.getClass());
    }

    public static Object convert(Object value, PropertyKey propertyKey,
                                 InputSource source) {
        E.checkArgumentNotNull(value, "The value to be converted can't be null");

        DataType dataType = propertyKey.dataType();
        Cardinality cardinality = propertyKey.cardinality();
        switch (cardinality) {
            case SINGLE:
                return parseSingleValue(value, dataType, source);
            case SET:
            case LIST:
                return parseMultiValues(value, dataType, cardinality, source);
            default:
                throw new AssertionError(String.format(
                          "Unsupported cardinality: '%s'", cardinality));
        }
    }

    public static long parseNumber(Object rawValue) {
        if (rawValue instanceof Number) {
            return ((Number) rawValue).longValue();
        } else if (rawValue instanceof String) {
            return parseLong(((String) rawValue).trim());
        }
        throw new IllegalArgumentException(String.format(
                  "The value must can be casted to Long, but got %s(%s)",
                  rawValue, rawValue.getClass().getName()));
    }

    public static UUID parseUUID(Object rawValue) {
        if (rawValue instanceof UUID) {
            return (UUID) rawValue;
        } else if (rawValue instanceof String) {
            String value = ((String) rawValue).trim();
            if (value.contains("-")) {
                return UUID.fromString(value);
            }
            // UUID represented by hex string
            E.checkArgument(value.length() == 32,
                            "Invalid UUID value: %s", value);
            String high = value.substring(0, 16);
            String low = value.substring(16);
            return new UUID(Long.parseUnsignedLong(high, 16),
                            Long.parseUnsignedLong(low, 16));
        }
        throw new IllegalArgumentException(String.format(
                  "Failed to convert value %s(%s) to UUID",
                  rawValue, rawValue.getClass()));
    }

    private static Object parseSingleValue(Object rawValue, DataType dataType,
                                           InputSource source) {
        // Trim space if raw value is string
        Object value = rawValue;
        if (rawValue instanceof String) {
            value = ((String) rawValue).trim();
        }
        if (dataType.isNumber()) {
            return parseNumber(value, dataType);
        } else if (dataType.isBoolean()) {
            return parseBoolean(value);
        } else if (dataType.isDate()) {
            E.checkState(source instanceof FileSource,
                         "Only accept FileSource when convert String value " +
                         "to Date, but got '%s'", source.getClass().getName());
            String dateFormat = ((FileSource) source).dateFormat();
            String timeZone = ((FileSource) source).timeZone();
            return parseDate(value, dateFormat, timeZone);
        } else if (dataType.isUUID()) {
            return parseUUID(value);
        }
        E.checkArgument(checkDataType(value, dataType),
                        "The value %s(%s) is not match with data type %s " +
                        "and can't convert to it",
                        value, value.getClass(), dataType);
        return value;
    }

    /**
     * collection format: "obj1,obj2,...,objn" or "[obj1,obj2,...,objn]" ..etc
     * TODO: After parsing to json, the order of the collection changed
     * in some cases (such as list<date>)
     **/
    private static Object parseMultiValues(Object values, DataType dataType,
                                           Cardinality cardinality,
                                           InputSource source) {
        // JSON file should not parse again
        if (values instanceof Collection &&
            checkCollectionDataType((Collection<?>) values, dataType)) {
            return values;
        }

        E.checkState(values instanceof String, "The value must be String type");
        String rawValue = (String) values;
        Collection<Object> valueColl = cardinality == Cardinality.LIST ?
                                       InsertionOrderUtil.newList() :
                                       InsertionOrderUtil.newSet();
        if (rawValue.isEmpty()) {
            return valueColl;
        }

        E.checkState(AbstractSource.class.isAssignableFrom(source.getClass()),
                     "Only accept AbstractSource when parse multi values, " +
                     "but got '%s'", source.getClass().getName());
        ListFormat listFormat = ((AbstractSource) source).listFormat();
        E.checkArgumentNotNull(listFormat, "The list_format must be set when " +
                                           "parse list or set values");

        String startSymbol = listFormat.startSymbol();
        String endSymbol = listFormat.endSymbol();
        E.checkArgument(rawValue.length() >=
                        startSymbol.length() + endSymbol.length(),
                        "The value '%s' length(%s) must be >= " +
                        "start symbol '%s' + end symbol '%s' length",
                        rawValue, rawValue.length(), startSymbol, endSymbol);
        E.checkArgument(rawValue.startsWith(startSymbol) &&
                        rawValue.endsWith(endSymbol),
                        "The value must start with '%s' and " +
                        "end with '%s', but got '%s'",
                        startSymbol, endSymbol, rawValue);
        rawValue = rawValue.substring(startSymbol.length(),
                                      rawValue.length() - endSymbol.length());

        String elemDelimiter = listFormat.elemDelimiter();
        Splitter.on(elemDelimiter).split(rawValue).forEach(value -> {
            if (!listFormat.ignoredElems().contains(value)) {
                valueColl.add(parseSingleValue(value, dataType, source));
            }
        });
        E.checkArgument(checkCollectionDataType(valueColl, dataType),
                        "Not all collection elems %s match with data type %s",
                        valueColl, dataType);
        return valueColl;
    }

    private static Boolean parseBoolean(Object rawValue) {
        if (rawValue instanceof Boolean) {
            return (Boolean) rawValue;
        }
        if (rawValue instanceof String) {
            String value = ((String) rawValue).toLowerCase();
            if (ACCEPTABLE_TRUE.contains(value)) {
                return true;
            } else if (ACCEPTABLE_FALSE.contains(value)) {
                return false;
            } else {
                throw new IllegalArgumentException(String.format(
                          "Failed to convert value %s to Boolean, "+
                          "the acceptable boolean strings are %s or %s",
                          rawValue, ACCEPTABLE_TRUE, ACCEPTABLE_FALSE));
            }
        }
        throw new IllegalArgumentException(String.format(
                  "Failed to convert value %s(%s) to Boolean",
                  rawValue, rawValue.getClass()));
    }

    private static Number parseNumber(Object value, DataType dataType) {
        E.checkState(dataType.isNumber(), "The target data type must be number");

        if (dataType.clazz().isInstance(value)) {
            return (Number) value;
        }
        try {
            switch (dataType) {
                case BYTE:
                    return Byte.valueOf(value.toString());
                case INT:
                    return Integer.valueOf(value.toString());
                case LONG:
                    return parseLong(value.toString());
                case FLOAT:
                    return Float.valueOf(value.toString());
                case DOUBLE:
                    return Double.valueOf(value.toString());
                default:
                    throw new AssertionError(String.format(
                              "Number type only contains Byte, Integer, " +
                              "Long, Float, Double, but got %s",
                              dataType.clazz()));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(
                      "Failed to convert value %s(%s) to Number",
                      value, value.getClass()), e);
        }
    }

    private static long parseLong(String rawValue) {
        if (rawValue.startsWith("-")) {
            return Long.parseLong(rawValue);
        } else {
            return Long.parseUnsignedLong(rawValue);
        }
    }

    private static Date parseDate(Object value, String dateFormat,
                                  String timeZone) {
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        } else if (value instanceof String) {
            try {
                return DateUtil.parse((String) value, dateFormat, timeZone);
            } catch (ParseException e) {
                throw new IllegalArgumentException(String.format(
                          "%s, expect format: %s",
                          e.getMessage(), DateUtil.toPattern(dateFormat)));
            }
        }
        throw new IllegalArgumentException(String.format(
                  "Failed to convert value %s(%s) to Date",
                  value, value.getClass()));
    }

    /**
     * Check type of the value valid
     */
    private static boolean checkDataType(Object value, DataType dataType) {
        if (value instanceof Number) {
            return parseNumber(value, dataType) != null;
        }
        return dataType.clazz().isInstance(value);
    }

    /**
     * Check type of all the values(may be some of list properties) valid
     */
    private static boolean checkCollectionDataType(Collection<?> values,
                                                   DataType dataType) {
        for (Object value : values) {
            if (!checkDataType(value, dataType)) {
                return false;
            }
        }
        return true;
    }
}
