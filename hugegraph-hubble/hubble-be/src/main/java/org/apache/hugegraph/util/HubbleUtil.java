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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;

public final class HubbleUtil {
    // Preserve Hubble's business dates without changing the JVM's time zone.
    public static final ZoneOffset TIME_ZONE = ZoneOffset.ofHours(8);
    private static final Clock CLOCK = Clock.system(TIME_ZONE);
    private static final DateTimeFormatter DATE_TIME_FORMAT = formatter("uuuu-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DAY_FORMAT = formatter("uuuuMMdd");
    private static final DateTimeFormatter COMPACT_DATE_TIME_FORMAT = formatter("uuuuMMdd HH:mm:ss");

    public static final Pattern HOST_PATTERN =
        Pattern.compile("(([0-9]{1,3}\\.){3}[0-9]{1,3}|" +
                        "([0-9A-Za-z_!~*'()-]+\\.)*[0-9A-Za-z_!~*'()-]+)$");

    private static DateTimeFormatter formatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern, Locale.ROOT)
                                .withResolverStyle(ResolverStyle.STRICT)
                                .withZone(TIME_ZONE);
    }

    /**
     * Isolates casts required by legacy client APIs that return raw JSON
     * containers. Callers remain responsible for using the documented type.
     */
    @SuppressWarnings("unchecked")
    public static <T> T uncheckedCast(Object value) {
        return (T) value;
    }

    public static String dateFormat() {
        return DATE_TIME_FORMAT.format(CLOCK.instant());
    }

    public static String dateFormatDay(Date date) {
        return DAY_FORMAT.format(date.toInstant());
    }

    public static String dateFormatDay(String date) {
        return LocalDate.parse(date, DAY_FORMAT).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String dateFormatLastDay() {
        return dateFormatLastDay(CLOCK);
    }

    static String dateFormatLastDay(Clock clock) {
        return LocalDate.now(clock.withZone(TIME_ZONE)).minusDays(1).format(DAY_FORMAT);
    }

    /**
     * Return the previous 24 hours and the current time in epoch seconds.
     */
    public static long[] getTimestampsBefore24Hours() {
        return timestampsBefore(CLOCK.instant(), Duration.ofHours(24));
    }

    public static long[] getTimestampsBefore24Hours(String time) {
        Instant end = LocalDateTime.parse(time, COMPACT_DATE_TIME_FORMAT).toInstant(TIME_ZONE);
        return timestampsBefore(end, Duration.ofHours(24));
    }

    private static long[] timestampsBefore(Instant end, Duration duration) {
        return new long[]{end.minus(duration).getEpochSecond(), end.getEpochSecond()};
    }

    public static Date nowDate() {
        return new Date();
    }

    public static Instant nowTime() {
        return Instant.now();
    }

    public static boolean equalCollection(Collection<?> c1, Collection<?> c2) {
        if (c1 != null && c2 == null || c1 == null && c2 != null) {
            return false;
        }
        return c1 == null || CollectionUtils.isEqualCollection(c1, c2);
    }

    public static String generateSimpleId() {
        return UUID.randomUUID().toString();
    }

    public static String md5(String rawText) {
        return DigestUtils.md5Hex(rawText);
    }

    public static String md5Secret(String rawText) {
        return md5("a1p" + md5(rawText).substring(5, 15) + "ck0").substring(1, 17);
    }
}
