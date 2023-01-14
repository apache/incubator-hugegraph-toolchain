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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.JulianFields;
import java.util.Date;

import org.apache.parquet.example.data.Group;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.Type;

import org.apache.hugegraph.loader.exception.LoadException;

public class ParquetUtil {

    public static Object convertObject(Group group, int fieldIndex) {
        Type fieldType = group.getType().getType(fieldIndex);
        if (!fieldType.isPrimitive()) {
            throw new LoadException("Unsupported rich object type %s",
                                    fieldType);
        }
        String fieldName = fieldType.getName();
        // Field is no value
        if (group.getFieldRepetitionCount(fieldName) == 0) {
            return null;
        }
        Object object;
        switch (fieldType.asPrimitiveType().getPrimitiveTypeName()) {
            case INT32:
                object = group.getInteger(fieldName, 0);
                break;
            case INT64:
                object = group.getLong(fieldName, 0);
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

    /*
     * Reference:https://stackoverflow.com/questions/53690299/int96value-to-date-string
     */
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

        // Find nanos since midday (since Julian days start at midday)
        long nanos = 0;
        // Continue from the index we got to
        while (index > 0) {
            index--;
            nanos <<= 8;
            nanos += int96Bytes[index] & 0xFF;
        }

        LocalDateTime timestamp = LocalDate.MIN.with(JulianFields.JULIAN_DAY,
                                                     julianDay)
                                               .atTime(LocalTime.NOON)
                                               .plusNanos(nanos);
        return Date.from(timestamp.atZone(ZoneId.systemDefault()).toInstant());
    }
}
