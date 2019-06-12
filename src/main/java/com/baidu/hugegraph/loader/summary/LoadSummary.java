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

package com.baidu.hugegraph.loader.summary;

import com.baidu.hugegraph.loader.constant.ElemType;

public final class LoadSummary {

    private final LoadMetrics vertexMetrics;
    private final LoadMetrics edgeMetrics;

    public LoadSummary() {
        this.vertexMetrics = new LoadMetrics(ElemType.VERTEX);
        this.edgeMetrics = new LoadMetrics(ElemType.EDGE);
    }

    public LoadMetrics metrics(ElemType type) {
        if (type.isVertex()) {
            return this.vertexMetrics;
        } else {
            assert type.isEdge();
            return this.edgeMetrics;
        }
    }

    public LoadMetrics vertexMetrics() {
        return this.vertexMetrics;
    }

    public LoadMetrics edgeMetrics() {
        return this.edgeMetrics;
    }
}
