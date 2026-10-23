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

package org.apache.hugegraph.util;

import org.apache.commons.lang3.StringUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public class ESUtil {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss", Locale.ROOT)
                             .withZone(HubbleUtil.TIME_ZONE);

    public static Object getValueByPath(Map<String, Object> map,
                                        String[] keys) {
        if (keys.length == 1) {
            return map.getOrDefault(keys[0], null);
        } else {
            Object data1 = map.get(keys[0]);
            if (data1 instanceof Map) {
                return getValueByPath(HubbleUtil.uncheckedCast(data1),
                                      Arrays.copyOfRange(keys, 1,
                                                         keys.length));
            }

            return data1;
        }
    }

    public static String parseTimestamp(String esTimestamp) {
        if (StringUtils.isEmpty(esTimestamp)) {
            return null;
        }
        ZonedDateTime dateTime =
                ZonedDateTime.parse(esTimestamp,
                                    DateTimeFormatter.ISO_DATE_TIME);
        return TIMESTAMP_FORMAT.format(dateTime);
    }
}
