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
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.structure.constant.Cardinality;
import com.baidu.hugegraph.structure.constant.DataType;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.util.E;
import com.google.common.base.Splitter;

public final class DataTypeUtil {

    public static Object convert(Object value, PropertyKey propertyKey,
                                 InputSource source) {
        if (value == null) {
            return null;
        }

        DataType dataType = propertyKey.dataType();
        Cardinality cardinality = propertyKey.cardinality();
        switch (cardinality) {
            case SINGLE:
                return parseSingleValue(dataType, value, source);
            case SET:
            case LIST:
                // TODO: diff SET & LIST (Server should support first)
                return parseMultiValues(value, dataType, source);
            default:
                throw new AssertionError(String.format(
                          "Unsupported cardinality: '%s'", cardinality));
//        } else if (propertyKey.cardinality() == Cardinality.SET) {
//            if (value instanceof String) {
//                return JsonUtil.fromJson((String) value, Set.class);
//            }
//        } else {
//            assert propertyKey.cardinality() == Cardinality.LIST;
//            if (value instanceof String) {
//                return JsonUtil.fromJson((String) value, List.class);
//            }
//>>>>>>> bdd70be... Support load data from HDFS and relational database
        }
    }

    private static Object parseSingleValue(DataType dataType, Object value,
                                           InputSource source) {
        if (dataType.isNumber()) {
            return valueToNumber(value, dataType);
        } else if (dataType.isDate()) {
            E.checkState(source instanceof FileSource,
                         "Only accept FileSource as an auxiliary when " +
                         "convert String value to Date, but got '%s'",
                         source.getClass().getName());
            String df = ((FileSource) source).dateFormat();
            return valueToDate(value, dataType, df);
        } else if (dataType.isUUID()) {
            return valueToUUID(value, dataType);
        }
        if (checkDataType(value, dataType)) {
            return value;
        }
        return null;
    }

    /**
     * collection format:
     * "obj1,obj2,...,objn" or "[obj1,obj2,...,objn]" ..etc
     * TODO: After parsing to json, the order of the collection changed in some cases (such as list<date>)
     **/
    private static Object parseMultiValues(Object values, DataType dataType,
                                           InputSource source,
                                           char... symbols) {
        // json file should not parse again
        if (values instanceof Collection &&
            checkCollectionDataType((Collection<?>) values, dataType)) {
            return values;
        }

        E.checkState(values instanceof String, "The value must be String type");
        String originValue = String.valueOf(values);
        List<Object> valueList = new LinkedList<>();
        // use custom start&end format :like [obj1,obj2,...,objn]
        if (symbols != null && symbols.length == 2 && originValue.charAt(0) ==
            symbols[0] && originValue.charAt(originValue.length() - 1) == symbols[1]) {
            originValue = originValue.substring(1, originValue.length() - 1);
        }
        // TODO: Separator should also be customizable
        Splitter.on(',').splitToList(originValue).forEach(value -> {
            valueList.add(parseSingleValue(dataType, value, source));
        });
        if (checkCollectionDataType(valueList, dataType)) {
            return valueList;
        }
        return null;
    }

    private static Number valueToNumber(Object value, DataType dataType) {
        E.checkState(dataType.isNumber(), "The target data type must be number");

        if (dataType.clazz().isInstance(value)) {
            return (Number) value;
        }

        Number number = null;
        try {
            switch (dataType) {
                case BYTE:
                    number = Byte.valueOf(value.toString());
                    break;
                case INT:
                    number = Integer.valueOf(value.toString());
                    break;
                case LONG:
                    number = Long.valueOf(value.toString());
                    break;
                case FLOAT:
                    number = Float.valueOf(value.toString());
                    break;
                case DOUBLE:
                    number = Double.valueOf(value.toString());
                    break;
                default:
                    throw new AssertionError(String.format(
                              "Number type only contains Byte, Integer, " +
                              "Long, Float, Double, but got %s",
                              dataType.clazz()));
            }
        } catch (NumberFormatException ignored) {
            // Unmatched type found
        }
        return number;
    }

    private static Date valueToDate(Object value, DataType dataType, String df) {
        if (value instanceof Date) {
            return (Date) value;
        }
        if (dataType.isDate()) {
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
        }
        return null;
    }

    private static UUID valueToUUID(Object value, DataType dataType) {
        if (value instanceof UUID) {
            return (UUID) value;
        }
        if (dataType.isUUID() && value instanceof String) {
            return UUID.fromString((String) value);
        }
        return null;
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
