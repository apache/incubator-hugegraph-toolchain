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

package org.apache.hugegraph.structure.gremlin;

import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;

public class Result {

    private Object object;

    public Result(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return this.object;
    }

    public String getString() {
        return this.object.toString();
    }

    public int getInt() {
        return Integer.parseInt(this.object.toString());
    }

    public byte getByte() {
        return Byte.parseByte(this.object.toString());
    }

    public short getShort() {
        return Short.parseShort(this.object.toString());
    }

    public long getLong() {
        return Long.parseLong(this.object.toString());
    }

    public float getFloat() {
        return Float.parseFloat(this.object.toString());
    }

    public double getDouble() {
        return Double.parseDouble(this.object.toString());
    }

    public boolean getBoolean() {
        return Boolean.parseBoolean(this.object.toString());
    }

    public boolean isNull() {
        return null == this.object;
    }

    public Vertex getVertex() {
        return (Vertex) this.object;
    }

    public Edge getEdge() {
        return (Edge) this.object;
    }

    public Path getPath() {
        return (Path) this.object;
    }
}
