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

package com.baidu.hugegraph.loader.progress;

import java.util.Set;

import com.baidu.hugegraph.loader.source.graph.ElementSource;
import com.baidu.hugegraph.loader.source.SourceType;
import com.baidu.hugegraph.util.InsertionOrderUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class InputProgress {

    @JsonProperty("type")
    private final SourceType type;
    @JsonProperty("loaded_items")
    private final Set<InputItem> loadedItems;
    @JsonProperty("loading_item")
    private InputItem loadingItem;

    @JsonCreator
    public InputProgress(@JsonProperty("type") SourceType type,
                         @JsonProperty("loaded_items") Set<InputItem> loadedItems,
                         @JsonProperty("loading_item") InputItem loadingItem) {
        this.type = type;
        this.loadedItems = loadedItems;
        this.loadingItem = loadingItem;
    }

    public InputProgress(ElementSource source) {
        this.type = source.input().type();
        this.loadedItems = InsertionOrderUtil.newSet();
        this.loadingItem = null;
    }

    public InputItem matchLoadedItem(InputItem inputItem) {
        for (InputItem item : this.loadedItems) {
            if (item.equals(inputItem)) {
                return item;
            }
        }
        return null;
    }

    public void addLoadedItem(InputItem inputItem) {
        this.loadedItems.add(inputItem);
    }

    public void addLoadingItem(InputItem inputItem) {
        this.loadingItem = inputItem;
    }

    public long loadingOffset() {
        return this.loadingItem == null ? 0L : this.loadingItem.offset();
    }

    public void increaseLoadingOffset() {
        assert this.loadingItem != null;
        this.loadingItem.increaseOffset();
    }

    public void addLoadingOffset(long increment) {
        assert this.loadingItem != null;
        this.loadingItem.addOffset(increment);
    }

    public void loadingItemMarkLoaded() {
        this.loadedItems.add(this.loadingItem);
        this.loadingItem = null;
    }
}
