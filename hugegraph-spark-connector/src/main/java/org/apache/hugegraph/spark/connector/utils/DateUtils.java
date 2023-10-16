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

package org.apache.hugegraph.spark.connector.utils;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hugegraph.date.SafeDateFormat;

public final class DateUtils {

    private static final Map<String, SafeDateFormat> DATE_FORMATS = new ConcurrentHashMap<>();

    public static Date parse(String source, String df, String timeZone) {
        SafeDateFormat dateFormat = getDateFormat(df);
        // parse date with specified timezone
        dateFormat.setTimeZone(timeZone);
        return dateFormat.parse(source);
    }

    private static SafeDateFormat getDateFormat(String df) {
        SafeDateFormat dateFormat = DATE_FORMATS.get(df);
        if (dateFormat == null) {
            dateFormat = new SafeDateFormat(df);
            SafeDateFormat previous = DATE_FORMATS.putIfAbsent(df, dateFormat);
            if (previous != null) {
                dateFormat = previous;
            }
        }
        return dateFormat;
    }
}
