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

package org.apache.hugegraph.loader.test.unit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.nio.ByteOrder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

import org.apache.hadoop.conf.Configuration;
import org.apache.hugegraph.loader.progress.InputItemProgress;
import org.apache.hugegraph.loader.reader.Readable;
import org.apache.hugegraph.loader.reader.file.ParquetFileLineFetcher;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.loader.util.DateUtil;
import org.apache.hugegraph.loader.util.ParquetUtil;
import org.apache.hugegraph.testutil.Assert;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;
import org.junit.Test;

public class ParquetUtilTest {

    private static final long JULIAN_DAY_AT_UNIX_EPOCH = 2440588L;

    @Test
    public void testInt96Midnight() {
        assertTimestamp(LocalDate.of(2019, 12, 9), LocalTime.MIDNIGHT);
    }

    @Test
    public void testInt96FractionalSecondsAcrossDayBoundary() {
        assertTimestamp(LocalDate.of(2020, 2, 29),
                        LocalTime.of(23, 59, 59, 123456789));
    }

    @Test
    public void testInt96BeforeUnixEpoch() {
        assertTimestamp(LocalDate.of(1969, 12, 31), LocalTime.MIDNIGHT);
    }

    @Test
    public void testLogicalDatesFromParquetFile() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        Path directory = Files.createTempDirectory("parquet-dates-");
        Path file = directory.resolve("dates.parquet");
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
            MessageType schema = MessageTypeParser.parseMessageType(
                    "message dates { " +
                    "required int32 day (DATE); " +
                    "required int64 millis (TIMESTAMP(MILLIS,true)); " +
                    "required int64 micros (TIMESTAMP(MICROS,true)); " +
                    "required int64 nanos (TIMESTAMP(NANOS,true)); " +
                    "required int64 local_micros (TIMESTAMP(MICROS,false)); " +
                    "optional int32 missing (DATE); " +
                    "required int32 number; " +
                    "required int64 time (TIME(MICROS,true)); }");
            Configuration conf = new Configuration();
            conf.set("fs.file.impl", "org.apache.hadoop.fs.RawLocalFileSystem");
            org.apache.hadoop.fs.Path path =
                    new org.apache.hadoop.fs.Path(file.toUri());
            long[] values = {-1L, 1500000000123456L};
            try (ParquetWriter<Group> writer = ExampleParquetWriter.builder(path)
                    .withConf(conf).withType(schema).build()) {
                for (long value : values) {
                    Group group = new SimpleGroupFactory(schema).newGroup();
                    group.add("day", value < 0 ? -1 : 17365);
                    group.add("millis", Math.floorDiv(value, 1000L));
                    group.add("micros", value);
                    group.add("nanos", value * 1000L + 999L);
                    group.add("local_micros", value);
                    group.add("number", 123);
                    group.add("time", 123456L);
                    writer.write(group);
                }
            }
            try (ParquetReader<Group> reader = ParquetReader
                    .builder(new GroupReadSupport(), path).withConf(conf).build()) {
                for (long value : values) {
                    Group group = reader.read();
                    long expectedMillis = Math.floorDiv(value, 1000L);
                    long expectedDay = LocalDate.ofEpochDay(value < 0 ? -1 : 17365)
                            .atStartOfDay(ZoneId.systemDefault()).toInstant()
                            .toEpochMilli();
                    assertDate(expectedDay, ParquetUtil.convertObject(group, 0));
                    assertDate(expectedMillis, ParquetUtil.convertObject(group, 1));
                    assertDate(expectedMillis, ParquetUtil.convertObject(group, 2));
                    assertDate(expectedMillis, ParquetUtil.convertObject(group, 3));
                    long expectedLocal = Instant.ofEpochMilli(expectedMillis)
                            .atOffset(ZoneOffset.UTC).toLocalDateTime()
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    assertDate(expectedLocal, ParquetUtil.convertObject(group, 4));
                    Assert.assertNull(ParquetUtil.convertObject(group, 5));
                    Assert.assertEquals(123, ParquetUtil.convertObject(group, 6));
                    Assert.assertEquals(123456L, ParquetUtil.convertObject(group, 7));
                }
                Assert.assertNull(reader.read());
            }
        } finally {
            TimeZone.setDefault(previous);
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void testParquetFetcherUsesSourceTimeZone() throws Exception {
        assertSourceTimeZone("GMT+8");
    }

    @Test
    public void testParquetFetcherAcceptsShortTimeZone() throws Exception {
        assertSourceTimeZone("PST");
    }

    private static void assertSourceTimeZone(String timeZone) throws Exception {
        TimeZone previous = TimeZone.getDefault();
        Path directory = Files.createTempDirectory("parquet-source-zone-");
        Path file = directory.resolve("dates.parquet");
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            Assert.assertTrue(DateUtil.checkTimeZone(timeZone));
            MessageType schema = MessageTypeParser.parseMessageType(
                    "message dates { " +
                    "required int32 day (DATE); " +
                    "required int64 local_micros (TIMESTAMP(MICROS,false)); " +
                    "required int64 utc_micros (TIMESTAMP(MICROS,true)); }");
            Configuration conf = new Configuration();
            conf.set("fs.file.impl", "org.apache.hadoop.fs.RawLocalFileSystem");
            org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(file.toUri());
            try (ParquetWriter<Group> writer = ExampleParquetWriter.builder(path)
                    .withConf(conf).withType(schema).build()) {
                Group group = new SimpleGroupFactory(schema).newGroup();
                group.add("day", -1);
                group.add("local_micros", -1L);
                group.add("utc_micros", -1L);
                writer.write(group);
            }
            FileSource source = new FileSource();
            source.timeZone(timeZone);
            source.header(new String[]{"day", "local_micros", "utc_micros"});
            Readable readable = new Readable() {
                @Override
                public String name() {
                    return file.getFileName().toString();
                }

                @Override
                public org.apache.hadoop.fs.Path path() {
                    return path;
                }

                @Override
                public InputStream open() throws IOException {
                    return Files.newInputStream(file);
                }

                @Override
                public InputItemProgress inputItemProgress() {
                    return null;
                }
            };
            ParquetFileLineFetcher fetcher = new ParquetFileLineFetcher(source, conf);
            fetcher.openReader(readable);
            try {
                Object[] values = fetcher.fetch().values();
                ZoneId sourceZone = TimeZone.getTimeZone(timeZone).toZoneId();
                assertDate(LocalDate.of(1969, 12, 31).atStartOfDay(sourceZone)
                                    .toInstant().toEpochMilli(), values[0]);
                assertDate(Instant.ofEpochMilli(-1L).atOffset(ZoneOffset.UTC)
                                  .toLocalDateTime().atZone(sourceZone)
                                  .toInstant().toEpochMilli(), values[1]);
                assertDate(-1L, values[2]);
                Assert.assertNull(fetcher.fetch());
            } finally {
                fetcher.closeReader();
            }
        } finally {
            TimeZone.setDefault(previous);
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static void assertDate(long expectedMillis, Object actual) {
        Assert.assertTrue(actual instanceof Date);
        Assert.assertEquals(expectedMillis, ((Date) actual).getTime());
    }

    private static void assertTimestamp(LocalDate day, LocalTime time) {
        MessageType schema = MessageTypeParser.parseMessageType(
                "message timestamp { required int96 value; }");
        Group group = new SimpleGroupFactory(schema).newGroup();
        // INT96 stores nanoseconds since midnight followed by the Julian day.
        int julianDay = Math.toIntExact(day.toEpochDay() +
                                       JULIAN_DAY_AT_UNIX_EPOCH);
        byte[] encoded = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
                                   .putLong(time.toNanoOfDay())
                                   .putInt(julianDay).array();
        group.add("value", Binary.fromConstantByteArray(encoded));

        Object actual = ParquetUtil.convertObject(group, 0);
        Assert.assertTrue(actual instanceof Date);
        long expected = day.atTime(time).atZone(ZoneId.systemDefault())
                           .toInstant().toEpochMilli();
        Assert.assertEquals(expected, ((Date) actual).getTime());
    }
}
