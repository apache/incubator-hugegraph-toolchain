/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Random;

import org.apache.hugegraph.serializer.BytesBuffer;
import org.apache.hugegraph.util.LZ4Util;

import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Factory;

public final class CompressionGrowthBenchmark {

    private static final class TrackedBuffer extends BytesBuffer {

        private long copied;
        private int growths;

        private TrackedBuffer(int capacity) {
            super(capacity);
        }

        private void track(int previousCapacity, int previousPosition) {
            if (this.array().length != previousCapacity) {
                this.growths++;
                this.copied += previousPosition;
            }
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            int capacity = this.array().length;
            int position = this.position();
            super.write(bytes, offset, length);
            this.track(capacity, position);
        }

        @Override
        public void write(byte[] bytes) {
            int capacity = this.array().length;
            int position = this.position();
            super.write(bytes);
            this.track(capacity, position);
        }

        @Override
        public void write(int value) {
            int capacity = this.array().length;
            int position = this.position();
            super.write(value);
            this.track(capacity, position);
        }
    }

    public static void main(String[] args) throws Exception {
        String variant = args[0];
        int size = Integer.parseInt(args[1]) * 1024 * 1024;
        byte[] input = new byte[size];
        new Random(7).nextBytes(input);
        for (int iteration = -1; iteration < 3; iteration++) {
            System.gc();
            TrackedBuffer buffer = new TrackedBuffer(Math.round(input.length / 1.5F));
            long start = System.nanoTime();
            try (LZ4BlockOutputStream out = new LZ4BlockOutputStream(
                    buffer, 4096, LZ4Factory.fastestInstance().fastCompressor())) {
                out.write(input);
            }
            byte[] compressed = buffer.bytes();
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            if (!Arrays.equals(input, LZ4Util.decompress(compressed, 4096).bytes())) {
                throw new AssertionError("Compression round trip differs");
            }
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                                                              .digest(compressed));
            System.out.printf(Locale.ROOT,
                    "{\"variant\":\"%s\",\"iteration\":%d,\"input_bytes\":%d," +
                    "\"compressed_bytes\":%d,\"ms\":%.3f,\"growths\":%d," +
                    "\"copied_prefix_bytes\":%d,\"sha256\":\"%s\"}%n",
                    variant, iteration, size, compressed.length, elapsedMs,
                    buffer.growths, buffer.copied, sha);
        }
    }
}
