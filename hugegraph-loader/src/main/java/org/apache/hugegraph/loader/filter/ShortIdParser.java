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

package org.apache.hugegraph.loader.filter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.filter.util.SegmentIdGenerator;
import org.apache.hugegraph.loader.filter.util.ShortIdConfig;
import org.apache.hugegraph.loader.util.DataTypeUtil;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.constant.DataType;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
// import org.apache.hugegraph.util.collection.JniBytes2BytesMap;

public class ShortIdParser implements ElementParser {
    private Map<String, String> labels;

    private Map<byte[], byte[]> map;

    private ThreadLocal<SegmentIdGenerator.Context> idPool;

    private SegmentIdGenerator segmentIdGenerator;

    private LoadOptions options;

    private Map<String, ShortIdConfig> configs;

    public ShortIdParser(LoadOptions options){
        this.options = options;
        this.labels = new HashMap<>();
        this.configs = convertShortIdConfigs();
        // TODO use JniBytes2BytesMap
        this.map = new HashMap<>();
        this.idPool = new ThreadLocal<>();
        this.segmentIdGenerator = new SegmentIdGenerator();
    }

    public Map<String, ShortIdConfig> convertShortIdConfigs() {
        Map<String, ShortIdConfig> map = new HashMap<>();
        for(ShortIdConfig config : options.shorterIDConfigs) {
            map.put(config.getVertexLabel(), config);
            labels.put(config.getVertexLabel(), config.getVertexLabel());
        }
        return map;
    }

    @Override
    public boolean parse(GraphElement element) {
        if (element instanceof Edge) {
            Edge edge = (Edge) element;
            String label;
            if ((label = labels.get(edge.sourceLabel())) != null) {
                ShortIdConfig config = configs.get(edge.sourceLabel());
                edge.sourceId(getVertexNewId(label, idToBytes(config,  edge.sourceId())));
            }
            if ((label = labels.get(edge.targetLabel())) != null) {
                ShortIdConfig config = configs.get(edge.targetLabel());
                edge.targetId(getVertexNewId(label, idToBytes(config, edge.targetId())));
            }
        } else /* vertex */ {
            Vertex vertex = (Vertex) element;
            if (configs.containsKey(vertex.label())) {
                ShortIdConfig config = configs.get(vertex.label());
                String idField = config.getIdFieldName();
                Object originId = vertex.id();
                if (originId == null){
                    originId = vertex.property(config.getPrimaryKeyField());
                }
                vertex.property(idField, originId);

                vertex.id(getVertexNewId(config.getVertexLabel(), idToBytes(config, originId)));
            }
        }
        return true;
    }

    int getVertexNewId(String label, byte[] oldId) {
        /* fix concat label*/
        byte[] key = oldId;
        byte[] value = map.get(key);
        if (value == null) {
            synchronized(this){
                if (!map.containsKey(key)){
                    /* gen id */
                    int id = newID();
                    /* save id */
                    map.put(stringToBytes(label + oldId), longToBytes(id));
                    return id;
                } else {
                    value = map.get(key);
                }
            }
        }
        return (int) bytesToLong(value);
    }

    public static byte[] idToBytes(ShortIdConfig config, Object obj) {
        DataType type = config.getIdFieldType();
        if (type.isText()) {
            String id = obj.toString();
            return id.getBytes(StandardCharsets.UTF_8);
        } else if (type.isUUID()) {
            UUID id = DataTypeUtil.parseUUID("Id", obj);
            byte[] b = new byte[16];
            return ByteBuffer.wrap(b)
                             .order(ByteOrder.BIG_ENDIAN)
                             .putLong(id.getMostSignificantBits())
                             .putLong(id.getLeastSignificantBits())
                             .array();
        } else if (type.isNumber()) {
            long id = DataTypeUtil.parseNumber("Id", obj);
            return longToBytes(id);
        }
        throw new LoadException("Unknow Id data type '%s'.", type.string());
    }

    public static byte[] stringToBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] longToBytes(long x) {
        return new byte[] {
                (byte) (x >>> 56),
                (byte) (x >>> 48),
                (byte) (x >>> 40),
                (byte) (x >>> 32),
                (byte) (x >>> 24),
                (byte) (x >>> 16),
                (byte) (x >>> 8),
                (byte) x};
    }

    public static long bytesToLong(byte[] bytes) {
        return (long) (bytes[0] << 56) |
               (long) (bytes[1] << 48) |
               (long) (bytes[2] << 40) |
               (long) (bytes[3] << 32) |
               (long) (bytes[4] << 24) |
               (long) (bytes[5] << 16) |
               (long) (bytes[6] << 8) |
               (long) bytes[7];
    }

    int newID() {
        SegmentIdGenerator.Context context = idPool.get();
        if (context == null) {
            context = segmentIdGenerator.genContext();
            idPool.set(context);
        }
        return context.next();
    }
}
