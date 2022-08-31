package com.baidu.hugegraph.serializer.direct;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.serializer.direct.struct.HugeType;
import com.baidu.hugegraph.serializer.direct.util.BytesBuffer;
import com.baidu.hugegraph.serializer.direct.util.GraphSchema;
import com.baidu.hugegraph.serializer.direct.util.Id;
import com.baidu.hugegraph.serializer.direct.util.IdGenerator;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.schema.PropertyKey;

import java.util.Arrays;
import java.util.Map;

public class HBaseSerializer {

    private int edgeLogicPartitions = 30;
    private int vertexLogicPartitions = 10;
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
            buffer.writeId(IdGenerator.of(graphSchema.getEdgeLabel(e.label()).id()));//出现错误
            buffer.writeStringWithEnding("");
            buffer.writeId(IdGenerator.of(edge.targetId()));
            array = buffer.bytes();
        }
        return array;
    }

    public byte[] getValueBytes(GraphElement e) {
        byte[] array = null;
        if(e.type() == "vertex"){
            int propsCount = e.properties().size() ;//vertex.sizeOfProperties();
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
