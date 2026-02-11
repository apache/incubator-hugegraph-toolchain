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

import java.util.ArrayList;
import java.util.List;

import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.structure.GraphElement;

public class ElementParseGroup {

    List<ElementParser> parser;

    private ElementParseGroup() {
        parser = new ArrayList<>();
    }

    public static ElementParseGroup create(LoadOptions options) {
        ElementParseGroup group = new ElementParseGroup();
        if (options.vertexEdgeLimit != -1L) {
            group.addFilter(new ElementLimitFilter(options.vertexEdgeLimit));
        }
        if (!options.shorterIDConfigs.isEmpty()) {
            group.addFilter(new ShortIdParser(options));
        }
        return group;
    }

    void addFilter(ElementParser filter) {
        parser.add(filter);
    }

    void removeFilter(ElementParser filter) {
        parser.remove(filter);
    }

    public boolean filter(GraphElement element) {
        for (ElementParser parser : parser) {
            boolean r = parser.parse(element);
            if (!r) {
                return false;
            }
        }
        return true;
    }

}
