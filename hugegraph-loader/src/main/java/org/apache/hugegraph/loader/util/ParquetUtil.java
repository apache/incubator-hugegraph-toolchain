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

package org.apache.hugegraph.loader.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.JulianFields;
import java.util.Date;

import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.LogicalTypeAnnotation.TimestampLogicalTypeAnnotation;
import org.apache.parquet.schema.Type;

public class ParquetUtil {

    public static Object convertObject(Group group, int fieldIndex) {
        return convertObject(group, fieldIndex, ZoneId.systemDefault());
    }

    public static Object convertObject(Group group, int fieldIndex, ZoneId timeZone) {
        Type fieldType = group.getType().getType(fieldIndex);
        if (!fieldType.isPrimitive()) {
            throw new LoadException("Unsupported rich object type %s", fieldType);
        }
        String fieldName = fieldType.getName();
        // Field is no value
        if (group.getFieldRepetitionCount(fieldName) == 0) {
            return null;
        }
        LogicalTypeAnnotation logicalType =
                fieldType.asPrimitiveType().getLogicalTypeAnnotation();
        Object object;
        switch (fieldType.asPrimitiveType().getPrimitiveTypeName()) {
            case INT32:
                int integer = group.getInteger(fieldName, 0);
                if (logicalType instanceof LogicalTypeAnnotation.DateLogicalTypeAnnotation) {
                    object = Date.from(LocalDate.ofEpochDay(integer)
                            .atStartOfDay(timeZone).toInstant());
                } else {
                    object = integer;
                }
                break;
            case INT64:
                long number = group.getLong(fieldName, 0);
                if (logicalType instanceof TimestampLogicalTypeAnnotation) {
                    object = dateFromTimestamp(number,
                            (TimestampLogicalTypeAnnotation) logicalType, timeZone);
                } else {
                    object = number;
                }
                break;
            case INT96:
                object = dateFromInt96(group.getInt96(fieldName, 0));
                break;
            case FLOAT:
                object = group.getFloat(fieldName, 0);
                break;
            case DOUBLE:
                object = group.getDouble(fieldName, 0);
                break;
            case BOOLEAN:
                object = group.getBoolean(fieldName, 0);
                break;
            default:
                object = group.getValueToString(fieldIndex, 0);
                break;
        }
        return object;
    }

    private static Date dateFromTimestamp(long value,
                                          TimestampLogicalTypeAnnotation type,
                                          ZoneId timeZone) {
        long unitsPerSecond;
        switch (type.getUnit()) {
            case MILLIS:
                unitsPerSecond = 1000L;
                break;
            case MICROS:
                unitsPerSecond = 1000000L;
                break;
            case NANOS:
                unitsPerSecond = 1000000000L;
                break;
            default:
                throw new LoadException("Unsupported timestamp unit %s", type.getUnit());
        }
        Instant instant = Instant.ofEpochSecond(
                Math.floorDiv(value, unitsPerSecond),
                Math.floorMod(value, unitsPerSecond) * (1000000000L / unitsPerSecond));
        if (!type.isAdjustedToUTC()) {
            // Local timestamps encode a wall-clock time in the source time zone.
            instant = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
                                   .atZone(timeZone).toInstant();
        }
        return Date.from(instant);
    }

    private static Date dateFromInt96(Binary value) {
        byte[] int96Bytes = value.getBytes();
        // Find Julian day
        int julianDay = 0;
        int index = int96Bytes.length;
        while (index > 8) {
            index--;
            julianDay <<= 8;
            julianDay += int96Bytes[index] & 0xFF;
        }

        // Parquet INT96 stores nanoseconds since midnight.
        long nanos = 0;
        // Continue from the index we got to
        while (index > 0) {
            index--;
            nanos <<= 8;
            nanos += int96Bytes[index] & 0xFF;
        }

        LocalDateTime timestamp = LocalDate.MIN.with(JulianFields.JULIAN_DAY,
                                                     julianDay)
                                               .atTime(LocalTime.MIDNIGHT)
                                               .plusNanos(nanos);
        return Date.from(timestamp.atZone(ZoneId.systemDefault()).toInstant());
    }
}
