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

package com.baidu.hugegraph.loader.source;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EdgeSource extends ElementSource {

    private final List<String> sourceFields;
    private final List<String> targetFields;

    public EdgeSource(String label, InputSource input,
                      List<String> sourceFields, List<String> targetFields,
                      Map<String, String> mapping,
                      Set<String> selected, Set<String> ignored,
                      Set<Object> nullValues) {
        super(label, input, mapping, selected, ignored, nullValues);
        this.sourceFields = sourceFields;
        this.targetFields = targetFields;
    }

    public List<String> sourceFields() {
        return this.sourceFields;
    }

    public List<String> targetFields() {
        return this.targetFields;
    }

    @Override
    public String toString() {
        return String.format("edge-source(%s)", this.label());
    }
}
