/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.loader.reader.kafka;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;
import java.util.Queue;

import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.exception.InitException;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.parser.CsvLineParser;
import org.apache.hugegraph.loader.parser.JsonLineParser;
import org.apache.hugegraph.loader.parser.LineParser;
import org.apache.hugegraph.loader.parser.TextLineParser;
import org.apache.hugegraph.loader.reader.AbstractReader;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.loader.source.file.FileFormat;
import org.apache.hugegraph.loader.source.kafka.KafkaSource;
import org.apache.hugegraph.util.Log;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

import lombok.SneakyThrows;

public class KafkaReader extends AbstractReader {

    private static final Logger LOG = Log.logger(KafkaReader.class);

    private final KafkaSource source;

    private final LineParser parser;
    private Queue<String> batch;

    private static final String BASE_CONSUMER_GROUP = "kafka-reader-base";
    private final KafkaConsumer dataConsumer;
    private final boolean earlyStop;
    private boolean emptyPoll;

    public KafkaReader(KafkaSource source) {
        this.source = source;

        this.dataConsumer = createKafkaConsumer();
        this.parser = createLineParser();
        this.earlyStop = source.isEarlyStop();
    }

    @Override
    public void init(LoadContext context,
                     InputStruct struct) throws InitException {
        this.progress(context, struct);
    }

    @Override
    public void confirmOffset() {
        // Do Nothing
    }

    @Override
    public void close() {
        this.dataConsumer.close();
    }

    @Override
    public boolean hasNext() {
        return !this.earlyStop || !this.emptyPoll;
    }

    @Override
    public Line next() {
        if (batch == null || batch.size() == 0) {
            batch = nextBatch();
        }

        String rawValue = batch.poll();
        if (rawValue != null) {
            return this.parser.parse(this.source.header(), rawValue);
        } else {
            this.emptyPoll = true;
        }

        return null;
    }

    private int getKafkaTopicPartitionCount() {
        Properties props = new Properties();
        props.put("bootstrap.servers", this.source.getBootstrapServer());
        props.put("group.id", BASE_CONSUMER_GROUP);

        KafkaConsumer<?, ?> consumer = new KafkaConsumer<>(props);
        int count = consumer.partitionsFor(this.source.getTopic()).size();
        consumer.close();

        return count;
    }

    private KafkaConsumer<String, String> createKafkaConsumer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", this.source.getBootstrapServer());
        props.put("max.poll.records", this.source.getBatchSize());
        props.put("group.id", this.source.getGroup());
        props.put("enable.auto.commit", Constants.KAFKA_AUTO_COMMIT);
        props.put("auto.commit.interval.ms", String.valueOf(Constants.KAFKA_AUTO_COMMIT_INTERVAL));
        props.put("session.timeout.ms", String.valueOf(Constants.KAFKA_SESSION_TIMEOUT));
        if (this.source.isFromBeginning()) {
            props.put("auto.offset.reset", Constants.KAFKA_EARLIEST_OFFSET);
        } else {
            props.put("auto.offset.reset", Constants.KAFKA_LATEST_OFFSET);
        }
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(ImmutableList.of(this.source.getTopic()));
        return consumer;
    }

    @SneakyThrows
    private Deque<String> nextBatch() {
        ConsumerRecords<String, String> records =
                dataConsumer.poll(Duration.ofMillis(Constants.KAFKA_POLL_DURATION));
        Deque<String> queue = new ArrayDeque<>(records.count());
        if (records.count() == 0) {
            Thread.sleep(Constants.KAFKA_POLL_GAP_INTERVAL);
        } else {
            for (ConsumerRecord<String, String> record : records) {
                queue.add(record.value());
            }
        }

        return queue;
    }

    private LineParser createLineParser() {
        FileFormat format = source.getFormat();
        switch (format) {
            case CSV:
                return new CsvLineParser();
            case TEXT:
                return new TextLineParser(source.getDelimiter());
            case JSON:
                return new JsonLineParser();
            default:
                throw new AssertionError(String.format(
                        "Unsupported file format '%s' of source '%s'",
                        format, source));
        }
    }
}
