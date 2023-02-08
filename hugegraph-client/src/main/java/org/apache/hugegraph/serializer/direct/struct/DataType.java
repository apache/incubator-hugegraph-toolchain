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

package org.apache.hugegraph.serializer.direct.struct;

import java.util.Date;
import java.util.UUID;

import org.apache.hugegraph.serializer.direct.util.HugeException;
import org.apache.hugegraph.serializer.direct.util.StringEncoding;
import org.apache.hugegraph.util.CollectionUtil;
import org.apache.hugegraph.util.DateUtil;
import org.apache.hugegraph.util.E;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public enum DataType {

    UNKNOWN(0, "unknown", Object.class),
    OBJECT(1, "object", Object.class),
    BOOLEAN(2, "boolean", Boolean.class),
    BYTE(3, "byte", Byte.class),
    INT(4, "int", Integer.class),
    LONG(5, "long", Long.class),
    FLOAT(6, "float", Float.class),
    DOUBLE(7, "double", Double.class),
    TEXT(8, "text", String.class),
    //BLOB(9, "blob", Blob.class),
    DATE(10, "date", Date.class),
    UUID(11, "uuid", UUID.class);

    private final byte code;
    private final String name;
    private final Class<?> clazz;

    static {
        register(DataType.class);
    }

    static Table<Class<?>, Byte, DataType> TABLE = HashBasedTable.create();

    static void register(Class<? extends DataType> clazz) {
        Object enums;
        try {
            enums = clazz.getMethod("values").invoke(null);
        } catch (Exception e) {
            throw new HugeException("DataType invalid", e);
        }
        for (DataType e : CollectionUtil.<DataType>toList(enums)) {
            TABLE.put(clazz, e.code(), e);
        }
    }

    static <T extends DataType> T fromCode(Class<T> clazz, byte code) {
        @SuppressWarnings("unchecked")
        T value = (T) TABLE.get(clazz, code);
        if (value == null) {
            E.checkArgument(false, "Can't construct %s from code %s",
                            clazz.getSimpleName(), code);
        }
        return value;
    }

    DataType(int code, String name, Class<?> clazz) {
        assert code < 256;
        this.code = (byte) code;
        this.name = name;
        this.clazz = clazz;
    }

    public byte code() {
        return this.code;
    }

    public String string() {
        return this.name;
    }

    public Class<?> clazz() {
        return this.clazz;
    }

    public boolean isText() {
        return this == DataType.TEXT;
    }

    public boolean isNumber() {
        return this == BYTE || this == INT || this == LONG ||
               this == FLOAT || this == DOUBLE;
    }

    public boolean isNumber4() {
        // Store index value of Byte using 4 bytes
        return this == BYTE || this == INT || this == FLOAT;
    }

    public boolean isNumber8() {
        return this == LONG || this == DOUBLE;
    }

    //public boolean isBlob() {
    //    return this == DataType.BLOB;
    //}

    public boolean isDate() {
        return this == DataType.DATE;
    }

    public boolean isUUID() {
        return this == DataType.UUID;
    }

    public <V> Number valueToNumber(V value) {
        if (!(this.isNumber() && value instanceof Number)) {
            return null;
        }
        if (this.clazz.isInstance(value)) {
            return (Number) value;
        }

        Number number;
        try {
            switch (this) {
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
                              "Long, Float, Double, but got %s", this.clazz()));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(
                      "Can't read '%s' as %s: %s",
                      value, this.name, e.getMessage()));
        }
        return number;
    }

    public <V> Date valueToDate(V value) {
        if (!this.isDate()) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        } else if (value instanceof Integer) {
            return new Date(((Number) value).intValue());
        } else if (value instanceof Long) {
            return new Date(((Number) value).longValue());
        } else if (value instanceof String) {
            return DateUtil.parse((String) value);
        }
        return null;
    }

    public <V> UUID valueToUUID(V value) {
        if (!this.isUUID()) {
            return null;
        }
        if (value instanceof UUID) {
            return (UUID) value;
        } else if (value instanceof String) {
            return StringEncoding.uuid((String) value);
        }
        return null;
    }

    public static DataType fromClass(Class<?> clazz) {
        for (DataType type : DataType.values()) {
            if (type.clazz() == clazz) {
                return type;
            }
        }
        throw new HugeException("Unknown clazz '%s' for DataType", clazz);
    }
}
