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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.Date;
import java.util.UUID;
import java.util.Calendar;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;

public final class HubbleUtil {
    static {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.of("+8")));
    }

    public static final Pattern HOST_PATTERN =
        Pattern.compile("(([0-9]{1,3}\\.){3}[0-9]{1,3}|" + "([0-9A-Za-z_!~*'()-]+\\.)*[0-9A-Za-z_!~*'()-]+)$");
    private static final String DF = "yyyy-MM-dd HH:mm:ss";

    private static final String M_FORMAT = "yyyyMMdd'T'HHmmssSSS";
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat(DF);

    public static String dateFormat() {
        return DATE_FORMAT.format(new Date());
    }

    public static String dateFormatMillis() {
        return (new SimpleDateFormat(M_FORMAT)).format(new Date());
    }

    public static String dateFormatMonth(Date date) {
        return (new SimpleDateFormat("yyyyMM").format(date));
    }

    public static String dateFormatDay(Date date) {
        return (new SimpleDateFormat("yyyyMMdd").format(date));
    }

    public static String dateFormatDay(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Date da = null;
        try {
            da = sdf.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return (new SimpleDateFormat("yyyy-MM-dd").format(da));
    }

    public static String dateFormatLastMonth() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);

        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        return (new SimpleDateFormat("yyyyMM").format(cal.getTime()));
    }

    public static String dateFormatLastDay() {
        Calendar cal = Calendar.getInstance();
        int da = cal.get(Calendar.DATE);
        cal.set(Calendar.DATE, da - 1);
        return (new SimpleDateFormat("yyyyMMdd").format(cal.getTime()));
    }



    /**
     * 获取当前时间往前推7天的时间戳数组(单位/秒)
     * @return 包含前7天时间戳和当前时间戳的数组
     */
    public static long[] getTimestampsBefore7Days() {
        Calendar cal = Calendar.getInstance();
        long currentTimestamp = cal.getTimeInMillis();
        // 将时间设置为当前时间往前推24小时
        cal.add(Calendar.HOUR_OF_DAY, -24 * 7);
        long timestampBefore24Hours = cal.getTimeInMillis();
        return new long[]{timestampBefore24Hours / 1000,
                          currentTimestamp / 1000};
    }

    /**
     * 获取当前时间往前推24小时的时间戳数组(单位/秒)
     * @return 包含前24小时时间戳和当前时间戳的数组
     */
    public static long[] getTimestampsBefore24Hours() {
        Calendar cal = Calendar.getInstance();
        long currentTimestamp = cal.getTimeInMillis();
        // 将时间设置为当前时间往前推24小时
        cal.add(Calendar.HOUR_OF_DAY, -24);
        long timestampBefore24Hours = cal.getTimeInMillis();
        return new long[]{timestampBefore24Hours / 1000,
                          currentTimestamp / 1000};
    }


    public static long[] getTimestampsBefore24Hours(String time) {
        Date date = null;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
            date = dateFormat.parse(time);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return getTimestampsBefore24Hours(date.getTime());
    }

    /**
     * 获取timestamp时间往前推24小时的时间戳数组(单位/秒)
     * @param timestamp 给定时间戳(单位/ms)
     * @return 包含前24小时时间戳和当前时间戳的数组
     */
    private static long[] getTimestampsBefore24Hours(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        // 将时间设置为当前时间往前推24小时
        cal.add(Calendar.HOUR_OF_DAY, -24);
        return new long[]{cal.getTimeInMillis() / 1000, timestamp / 1000};
    }


    /**
     * 获取给定日期所在周的周一和周日的时间戳数组(单位/秒)
     * @param date 给定日期
     * @return 包含周一和周日时间戳的数组
     */
    public static long[] getWeekTimestamps(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        resetTime(cal);
        long startOfWeek = cal.getTimeInMillis();
        // 将日期调整到下周一
        cal.add(Calendar.DAY_OF_WEEK, 7);
        resetTime(cal);
        cal.add(Calendar.SECOND, -1);
        long endOfWeek = cal.getTimeInMillis();
        return new long[]{startOfWeek / 1000, endOfWeek / 1000};
    }


    /**
     * 获取给定日期所在月份的第一天和最后一天的时间戳数组(单位/秒)
     * @param date 给定日期
     * @return 包含月份第一天和最后一天时间戳的数组
     */
    public static long[] getMonthTimestamps(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        // 将日期调整到该月的第一天
        cal.set(Calendar.DAY_OF_MONTH, 1);
        resetTime(cal);
        long startOfMonth = cal.getTimeInMillis();
        // 将日期调整到下个月的第一天
        cal.add(Calendar.MONTH, 1);
        resetTime(cal);
        // 将日期调整到本月的最后一天
        cal.add(Calendar.SECOND, -1);
        // 获取月份的结束时间戳
        long endOfMonth = cal.getTimeInMillis();
        return new long[]{startOfMonth, endOfMonth};
    }

    /**
     * 重置时间为当天的开始时间（即00:00:00）
     */
    private static void resetTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
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
