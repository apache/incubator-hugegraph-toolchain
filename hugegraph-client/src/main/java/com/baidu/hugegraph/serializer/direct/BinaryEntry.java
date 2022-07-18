/*
 * Copyright 2017 HugeGraph Authors
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

package com.baidu.hugegraph.serializer.direct;

import static com.baidu.hugegraph.serializer.direct.util.StringEncoding.decode;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.baidu.hugegraph.serializer.direct.struct.HugeType;
import com.baidu.hugegraph.serializer.direct.util.BytesBuffer;
import com.baidu.hugegraph.serializer.direct.util.Id;
import com.baidu.hugegraph.util.Bytes;
import com.baidu.hugegraph.util.E;

public class BinaryEntry {

    private final HugeType type;
    private final BinaryId id;
    private Id subId;
    private final List<BackendColumn> columns;

    public static final long COMMIT_BATCH = 500L;

    public BinaryEntry(HugeType type, byte[] bytes) {
        this(type, BytesBuffer.wrap(bytes).parseId(type, false));
    }

    public BinaryEntry(HugeType type, byte[] bytes, boolean enablePartition) {
        this(type, BytesBuffer.wrap(bytes).parseId(type, enablePartition));
    }

    public BinaryEntry(HugeType type, BinaryId id) {
        this.type = type;
        this.id = id;
        this.subId = null;
        this.columns = new ArrayList<>();
    }

    public HugeType type() {
        return this.type;
    }

    public BinaryId id() {
        return this.id;
    }

    public Id originId() {
        return this.id.origin();
    }

    public Id subId() {
        return this.subId;
    }

    public void subId(Id subId) {
        this.subId = subId;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", this.id, this.columns.toString());
    }

    public BackendColumn column(byte[] name) {
        for (BackendColumn col : this.columns) {
            if (Bytes.equals(col.name, name)) {
                return col;
            }
        }
        return null;
    }

    public void column(BackendColumn column) {
        this.columns.add(column);
    }

    public void column(byte[] name, byte[] value) {
        E.checkNotNull(name, "name");
        value = value != null ? value : BytesBuffer.BYTES_EMPTY;
        this.columns.add(BackendColumn.of(name, value));
    }

    public Collection<BackendColumn> columns() {
        return Collections.unmodifiableList(this.columns);
    }

    public int columnsSize() {
        return this.columns.size();
    }

    public void columns(Collection<BackendColumn> bytesColumns) {
        this.columns.addAll(bytesColumns);
    }

    public void columns(BackendColumn bytesColumn) {
        this.columns.add(bytesColumn);
        long maxSize = COMMIT_BATCH;
        if (this.columns.size() > maxSize) {
            E.checkState(false, "Too many columns in one entry: %s", maxSize);
        }
    }

    public BackendColumn removeColumn(int index) {
        return this.columns.remove(index);
    }

    public void clear() {
        this.columns.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BinaryEntry)) {
            return false;
        }
        BinaryEntry other = (BinaryEntry) obj;
        if (this.id() != other.id() && !this.id().equals(other.id())) {
            return false;
        }
        if (this.columns.size() != other.columns.size()) {
            return false;
        }
        return new HashSet<>(this.columns).containsAll(other.columns);
    }

    @Override
    public int hashCode() {
        return this.id().hashCode() ^ this.columns.size();
    }

    protected static final class BinaryId implements Id {

        private final byte[] bytes;
        private final Id id;

        public BinaryId(byte[] bytes, Id id) {
            this.bytes = bytes;
            this.id = id;
        }

        public Id origin() {
            return this.id;
        }

        @Override
        public byte[] asBytes() {
            return this.bytes;
        }

        @Override
        public IdType type() {
            return IdType.UNKNOWN;
        }

        @Override
        public boolean edge() {
            return Id.super.edge();
        }

        public byte[] asBytes(int offset) {
            E.checkArgument(offset < this.bytes.length,
                            "Invalid offset %s, must be < length %s",
                            offset, this.bytes.length);
            return Arrays.copyOfRange(this.bytes, offset, this.bytes.length);
        }

        @Override
        public int length() {
            return this.bytes.length;
        }

        @Override
        public int hashCode() {
            return ByteBuffer.wrap(this.bytes).hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof BinaryId)) {
                return false;
            }
            return Arrays.equals(this.bytes, ((BinaryId) other).bytes);
        }

        @Override
        public String toString() {
            return "0x" + Bytes.toHex(this.bytes);
        }
    }

    static class BackendColumn implements Comparable<BackendColumn> {

            public byte[] name;
            public byte[] value;

            public static BackendColumn of(byte[] name, byte[] value) {
                BackendColumn col = new BackendColumn();
                col.name = name;
                col.value = value;
                return col;
            }

            @Override
            public String toString() {
                return String.format("%s=%s", decode(name), decode(value));
            }

            @Override
            public int compareTo(BackendColumn other) {
                if (other == null) {
                    return 1;
                }
                return Bytes.compare(this.name, other.name);
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof BackendColumn)) {
                    return false;
                }
                BackendColumn other = (BackendColumn) obj;
                return Bytes.equals(this.name, other.name) &&
                       Bytes.equals(this.value, other.value);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(this.name) ^
                       Arrays.hashCode(this.value);
            }

        }
}
