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

package org.apache.hugegraph.serializer.direct;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeMap;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.rest.RestClientConfig;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.serializer.AbstractGraphElementSerializer;
import org.apache.hugegraph.serializer.direct.struct.HugeType;

import org.apache.hugegraph.serializer.direct.util.*;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.util.PartitionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 序列化器，需要明确的是点边的序列化中是不带有分区信息的？
 */
public class HStoreSerializer extends AbstractGraphElementSerializer {




    private static final Logger log = LoggerFactory.getLogger(HStoreSerializer.class);
    // 准备好treeRangeMap

    // 准备好partGraphIdMap
    private static final ObjectMapper MAPPER = new ObjectMapper();


    private Map<Long,Long> partGraphIdMap;
    private TreeRangeMap<Long, Integer> rangeMap;

    public HStoreSerializer(HugeClient client, int numPartitions,String graphName,String pdAddress,String pdRestPort) {
        super(client);
        rangeMap = TreeRangeMap.create();
        int partitionSize = PartitionUtils.MAX_VALUE / numPartitions;
        if (PartitionUtils.MAX_VALUE % numPartitions != 0) {
            // 有余数，分区除不尽
            partitionSize++;
        }

        for (int i = 0; i < numPartitions; i++) {
            long startKey = (long) partitionSize * i;
            long endKey = (long) partitionSize * (i + 1);
            rangeMap.put(Range.closedOpen(startKey, endKey), i);
        }
        log.info("rangeMap:{}", rangeMap);
        partGraphIdMap=getGraphId(graphName,processAddresses(pdAddress,pdRestPort));
        log.info("partGraphIdMap:{}", partGraphIdMap);

    }
    public static String[] processAddresses(String addresses, String newPort) {
        // 使用逗号分割字符串
        String[] addressArray = addresses.split(",");

        // 创建一个新的数组来存储处理后的地址
        String[] processedArray = new String[addressArray.length];

        // 遍历数组并替换端口
        for (int i = 0; i < addressArray.length; i++) {
            String address = addressArray[i];
            // 找到冒号的位置
            int colonIndex = address.indexOf(":");
            if (colonIndex != -1) {
                // 替换端口部分
                String newAddress = "http://"+address.substring(0, colonIndex + 1) + newPort;
                processedArray[i] = newAddress;
            } else {
                // 如果没有冒号，直接使用原地址
                processedArray[i] = address;
            }
        }

        return processedArray;
    }

    public static void main(String[] args) {
//        Map graphId = getGraphId("hugegraph", new String[]{"http://10.150.17.39:8620"});
//        System.out.println(graphId);
    }

    private    Map<Long,Long>  getGraphId(String graphName,String[] urls) {
        RestClientConfig config = RestClientConfig.builder()
                .connectTimeout(5*1000)  // 连接超时时间 5s
//                .readTimeout(60*60 * 1000) // 读取超时时间 1h
                .maxConns(10) // 最大连接数
                .build();


        for (String url : urls) {
            log.info("getGraphId from {}, graphName:{}", url, graphName);
            RestClient client = null;
            try {
                // 创建RestClient对象
                client = new RestClient(url, config);
                RestResult restResult = client.get("v1/partitionsAndGraphId" , Collections.singletonMap("graphName", graphName));
                // 获取响应状态码
                String content = restResult.content();
                Map<Long, Long> resMap = MAPPER.readValue(content, new TypeReference<HashMap<Long, Long>>() {
                });
                log.info("Response :{} ", resMap);
                // 如果成功，退出循环
                return resMap;
            } catch (Exception e) {
                log.error("Failed to get graphId", e);
                System.out.println(e);
                break;
            } finally {
                // 确保RestClient被关闭
                if (client != null) {
                    try {
                        client.close();
                    } catch (Exception e) {
                        log.error("Failed to close RestClient", e);
                    }
                }
            }
        }
        return Collections.emptyMap();
    }

    public Tuple2<byte[], Integer> getKeyBytes(GraphElement e) {
        byte[] array = null;
        if (e.type() == "vertex" && e.id() != null) {

            BytesBuffer buffer = BytesBuffer.allocate( 2+1 + e.id().toString().length());
            Id id = IdGenerator.of(e.id());
            buffer.writeId(id);
            array = buffer.bytes();
            int code = PartitionUtils.calcHashcode(id.asBytes());
            log.info("code:{}", code);
            byte[] buf = new byte[Short.BYTES + array.length + Short.BYTES];
            // 基于code先拿到partId，然后再基于partId 获取到graphId
            Integer partId = rangeMap.get((long) code);
            log.info("partId:{}", partId);
            Long partGraphId = partGraphIdMap.get((long) partId);
            // 此处需要加入一个graphId 先默认给 0
            Bits.putShort(buf, 0, Math.toIntExact(partGraphId));
            Bits.put(buf, Short.BYTES, array);
            // code是基于key计算的一个hash值？  code
            Bits.putShort(buf, array.length + Short.BYTES, code);
            return new Tuple2<>(buf, partId);
        } else if (e.type() == "edge") {
            BytesBuffer buffer = BytesBuffer.allocate(BytesBuffer.BUF_EDGE_ID);
            Edge edge = (Edge)e;
//            buffer.writeShort();
            buffer.writeId(IdGenerator.of(edge.sourceId()));
            buffer.write(HugeType.EDGE_OUT.code());
            buffer.writeId(IdGenerator.of(graphSchema.getEdgeLabel(e.label()).id())); //出现错误
            buffer.writeStringWithEnding("");
            buffer.writeId(IdGenerator.of(edge.targetId()));
            array = buffer.bytes();

            // 基于code先拿到partId，然后再基于partId 获取到graphId
            int code = PartitionUtils.calcHashcode(IdGenerator.of(edge.sourceId()).asBytes());
            Integer partId = rangeMap.get((long) code);
            Long partGraphId = partGraphIdMap.get((long) partId);
            byte[] buf = new byte[Short.BYTES + array.length + Short.BYTES];
            // 此处需要加入一个graphId 先默认给 0
            Bits.putShort(buf, 0, Math.toIntExact(partGraphId));
            Bits.put(buf, Short.BYTES, array);
            // code是基于key计算的一个hash值？  code
            Bits.putShort(buf, array.length + Short.BYTES, code);
            return new Tuple2<>(buf, code);
        }
        return new Tuple2<>(array, 0);
    }


    public byte[] getValueBytes(GraphElement e) {
        byte[] array = null;
        if (e.type() == "vertex") {
            int propsCount = e.properties().size(); //vertex.sizeOfProperties();
            BytesBuffer buffer = BytesBuffer.allocate(8 + 16 * propsCount);
            buffer.writeId(IdGenerator.of(graphSchema.getVertexLabel(e.label()).id()));
            buffer.writeVInt(propsCount);
            for (Map.Entry<String, Object> entry : e.properties().entrySet()) {
                PropertyKey propertyKey = graphSchema.getPropertyKey(entry.getKey());
                buffer.writeVInt(propertyKey.id().intValue());
                buffer.writeProperty(propertyKey.dataType(),entry.getValue());
            }
            array = buffer.bytes();
        } else if (e.type() == "edge") {
            int propsCount =  e.properties().size();
            BytesBuffer buffer = BytesBuffer.allocate(4 + 16 * propsCount);
            buffer.writeVInt(propsCount);
            for (Map.Entry<String, Object> entry : e.properties().entrySet()) {
                PropertyKey propertyKey = graphSchema.getPropertyKey(entry.getKey());
                buffer.writeVInt(propertyKey.id().intValue());
                buffer.writeProperty(propertyKey.dataType(),entry.getValue());
            }
            array = buffer.bytes();
        }

        return array;
    }




}
