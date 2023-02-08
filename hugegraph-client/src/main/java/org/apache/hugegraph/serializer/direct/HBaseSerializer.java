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

import java.util.Arrays;
import java.util.Map;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.serializer.direct.struct.HugeType;
import org.apache.hugegraph.serializer.direct.util.BytesBuffer;
import org.apache.hugegraph.serializer.direct.util.GraphSchema;
import org.apache.hugegraph.serializer.direct.util.Id;
import org.apache.hugegraph.serializer.direct.util.IdGenerator;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.schema.PropertyKey;

/**
 * TODO: review later
 */
public class HBaseSerializer {

    private int edgeLogicPartitions;
    private int vertexLogicPartitions;
    private HugeClient client;
    private GraphSchema graphSchema;


    public HBaseSerializer(HugeClient client, int vertexPartitions, int edgePartitions){
        this.client = client;
        this.graphSchema = new GraphSchema(client);
        this.edgeLogicPartitions = edgePartitions;
        this.vertexLogicPartitions = vertexPartitions;
    }

    public byte[] getKeyBytes(GraphElement e) {
        byte[] array = null;
        if(e.type() == "vertex" && e.id() != null){
            BytesBuffer buffer = BytesBuffer.allocate(2 + 1 + e.id().toString().length());
            buffer.writeShort(getPartition(HugeType.VERTEX,  IdGenerator.of(e.id())));
            buffer.writeId(IdGenerator.of(e.id()));
            array = buffer.bytes();
        }else if ( e.type() == "edge" ){
            BytesBuffer buffer = BytesBuffer.allocate(BytesBuffer.BUF_EDGE_ID);
            Edge edge = (Edge)e;
            buffer.writeShort(getPartition(HugeType.EDGE, IdGenerator.of(edge.sourceId())));
            buffer.writeId(IdGenerator.of(edge.sourceId()));
            buffer.write(HugeType.EDGE_OUT.code());
            buffer.writeId(IdGenerator.of(graphSchema.getEdgeLabel(e.label()).id())); //出现错误
            buffer.writeStringWithEnding("");
            buffer.writeId(IdGenerator.of(edge.targetId()));
            array = buffer.bytes();
        }
        return array;
    }

    public byte[] getValueBytes(GraphElement e) {
        byte[] array = null;
        if(e.type() == "vertex"){
            int propsCount = e.properties().size() ; //vertex.sizeOfProperties();
            BytesBuffer buffer = BytesBuffer.allocate(8 + 16 * propsCount);
            buffer.writeId(IdGenerator.of(graphSchema.getVertexLabel(e.label()).id()));
            buffer.writeVInt(propsCount);
            for(Map.Entry<String, Object> entry : e.properties().entrySet()){
                PropertyKey propertyKey = graphSchema.getPropertyKey(entry.getKey());
                buffer.writeVInt(propertyKey.id().intValue());
                buffer.writeProperty(propertyKey.dataType(),entry.getValue());
            }
            array = buffer.bytes();
        } else if ( e.type() == "edge" ){
            int propsCount =  e.properties().size();
            BytesBuffer buffer = BytesBuffer.allocate(4 + 16 * propsCount);
            buffer.writeVInt(propsCount);
            for(Map.Entry<String, Object> entry : e.properties().entrySet()){
                PropertyKey propertyKey = graphSchema.getPropertyKey(entry.getKey());
                buffer.writeVInt(propertyKey.id().intValue());
                buffer.writeProperty(propertyKey.dataType(),entry.getValue());
            }
            array = buffer.bytes();
        }

        return array;
    }

    public short getPartition(HugeType type, Id id) {
        int hashcode = Arrays.hashCode(id.asBytes());
        short partition = 1;
        if (type.isEdge()) {
            partition = (short) (hashcode % edgeLogicPartitions);
        } else if (type.isVertex()) {
            partition = (short) (hashcode % vertexLogicPartitions);
        }
        return partition > 0 ? partition : (short) -partition;
    }

    public int getEdgeLogicPartitions(){
        return this.edgeLogicPartitions;
    }

    public int getVertexLogicPartitions(){
        return this.vertexLogicPartitions;
    }

    public void close(){
        this.client.close();
    }
}
