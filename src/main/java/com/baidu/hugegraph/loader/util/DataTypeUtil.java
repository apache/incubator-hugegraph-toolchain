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
import com.google.common.base.Splitter;

public final class DataTypeUtil {

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

    private static Object parseSingleValue(Object value, DataType dataType,
                                           InputSource source) {
        if (dataType.isNumber()) {
            return valueToNumber(value, dataType);
        } else if (dataType.isDate()) {
            E.checkState(source instanceof FileSource,
                         "Only accept FileSource when convert String value " +
                         "to Date, but got '%s'", source.getClass().getName());
            String df = ((FileSource) source).dateFormat();
            return valueToDate(value, df);
        } else if (dataType.isUUID()) {
            return valueToUUID(value);
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

    private static Number valueToNumber(Object value, DataType dataType) {
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
                    return Long.valueOf(value.toString());
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

    private static Date valueToDate(Object value, String df) {
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        } else if (value instanceof String) {
            try {
                return DateUtil.parse((String) value, df);
            } catch (ParseException e) {
                throw new IllegalArgumentException(String.format(
                          "%s, expect format: %s",
                          e.getMessage(), DateUtil.toPattern(df)));
            }
        }
        throw new IllegalArgumentException(String.format(
                  "Failed to convert value %s(%s) to Date",
                  value, value.getClass()));
    }

    private static UUID valueToUUID(Object value) {
        if (value instanceof UUID) {
            return (UUID) value;
        } else if (value instanceof String) {
            return UUID.fromString((String) value);
        }
        throw new IllegalArgumentException(String.format(
                  "Failed to convert value %s(%s) to UUID",
                  value, value.getClass()));
    }

    /**
     * Check type of the value valid
     */
    private static boolean checkDataType(Object value, DataType dataType) {
        if (value instanceof Number) {
            return valueToNumber(value, dataType) != null;
        }
        return dataType.clazz().isInstance(value);
    }

    /**
     * Check type of all the values(may be some of list properties) valid
     */
    private static boolean checkCollectionDataType(Collection<?> values,
                                                   DataType dataType) {
        boolean valid = true;
        for (Object value : values) {
            if (!checkDataType(value, dataType)) {
                valid = false;
                break;
            }
        }
        return valid;
    }
}
