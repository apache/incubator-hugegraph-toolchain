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

import java.util.Collections;
import java.util.Map;

import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.reader.Readable;
import com.baidu.hugegraph.loader.source.SourceType;
import com.baidu.hugegraph.util.InsertionOrderUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class InputProgress {

    @JsonProperty("type")
    private final SourceType type;
    @JsonProperty("loaded_items")
    private final Map<String, InputItemProgress> loadedItems;
    @JsonProperty("loading_items")
    private Map<String, InputItemProgress> loadingItems;

    @JsonCreator
    public InputProgress(@JsonProperty("type") SourceType type,
                         @JsonProperty("loaded_items")
                         Map<String, InputItemProgress> loadedItems,
                         @JsonProperty("loading_items")
                         Map<String, InputItemProgress> loadingItems) {
        this.type = type;
        this.loadedItems = loadedItems;
        this.loadingItems = loadingItems;
    }

    public InputProgress(InputStruct struct) {
        this.type = struct.input().type();
        this.loadedItems = Collections.synchronizedMap(
                           InsertionOrderUtil.newMap());
        this.loadingItems = Collections.synchronizedMap(
                            InsertionOrderUtil.newMap());
    }

    public Map<String, InputItemProgress> loadedItems() {
        return this.loadedItems;
    }

    public Map<String, InputItemProgress> loadingItems() {
        return this.loadingItems;
    }

    public InputItemProgress loadedItem(String name) {
        return this.loadedItems.get(name);
    }

    public InputItemProgress loadingItem(String name) {
        return this.loadingItems.get(name);
    }

    public InputItemProgress matchLoadedItem(InputItemProgress inputItem) {
        for (InputItemProgress item : this.loadedItems.values()) {
            if (item.equals(inputItem)) {
                return item;
            }
        }
        return null;
    }

    public InputItemProgress matchLoadingItem(InputItemProgress inputItem) {
        for (InputItemProgress item : this.loadingItems.values()) {
            if (item.equals(inputItem)) {
                return item;
            }
        }
        return null;
    }

    public synchronized void addLoadedItem(
                             String name, InputItemProgress inputItemProgress) {
        this.loadedItems.put(name,  inputItemProgress);
    }

    public synchronized void addLoadingItem(
                             String name, InputItemProgress inputItemProgress) {
        this.loadingItems.put(name, inputItemProgress);
    }

    public synchronized void markLoaded(Readable readable, boolean markAll) {
        if (!markAll) {
            return;
        }
        if (readable != null) {
            String name = readable.name();
            InputItemProgress item = this.loadingItems.remove(name);
            if (item != null) {
                this.loadedItems.put(name, item);
            }
            return;
        }
        if (!this.loadingItems.isEmpty()) {
            this.loadedItems.putAll(this.loadingItems);
            this.loadingItems.clear();
        }
    }

    public void confirmOffset() {
        for (InputItemProgress item : this.loadingItems.values()) {
            item.confirmOffset();
        }
    }
}
