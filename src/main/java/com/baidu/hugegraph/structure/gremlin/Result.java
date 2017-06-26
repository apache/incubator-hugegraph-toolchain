package com.baidu.hugegraph.structure.gremlin;

import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Path;
import com.baidu.hugegraph.structure.graph.Vertex;

/**
 * Created by liningrui on 2017/6/27.
 */
public class Result {

    private Object object;

    public Result(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public String getString() {
        return object.toString();
    }

    public int getInt() {
        return Integer.parseInt(object.toString());
    }

    public byte getByte() {
        return Byte.parseByte(object.toString());
    }

    public short getShort() {
        return Short.parseShort(object.toString());
    }

    public long getLong() {
        return Long.parseLong(object.toString());
    }

    public float getFloat() {
        return Float.parseFloat(object.toString());
    }

    public double getDouble() {
        return Double.parseDouble(object.toString());
    }

    public boolean getBoolean() {
        return Boolean.parseBoolean(object.toString());
    }

    public boolean isNull() {
        return null == object;
    }

    public Vertex getVertex() {
        return (Vertex) object;
    }

    public Edge getEdge() {
        return (Edge) object;
    }

    public Path getPath() {
        return (Path) object;
    }
}
