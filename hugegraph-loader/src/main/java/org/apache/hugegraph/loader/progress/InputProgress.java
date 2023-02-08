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

package org.apache.hugegraph.loader.progress;

import java.util.Set;

import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.source.SourceType;
import org.apache.hugegraph.util.InsertionOrderUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class InputProgress {

    @JsonProperty("type")
    private final SourceType type;
    @JsonProperty("loaded_items")
    private final Set<InputItemProgress> loadedItems;
    @JsonProperty("loading_item")
    private InputItemProgress loadingItem;

    private final transient Set<InputItemProgress> loadingItems;

    @JsonCreator
    public InputProgress(@JsonProperty("type") SourceType type,
                         @JsonProperty("loaded_items")
                         Set<InputItemProgress> loadedItems,
                         @JsonProperty("loading_item")
                         InputItemProgress loadingItem) {
        this.type = type;
        this.loadedItems = loadedItems;
        this.loadingItem = loadingItem;
        this.loadingItems = InsertionOrderUtil.newSet();
    }

    public InputProgress(InputStruct struct) {
        this.type = struct.input().type();
        this.loadedItems = InsertionOrderUtil.newSet();
        this.loadingItem = null;
        this.loadingItems = InsertionOrderUtil.newSet();
    }

    public Set<InputItemProgress> loadedItems() {
        return this.loadedItems;
    }

    public InputItemProgress loadingItem() {
        return this.loadingItem;
    }

    public InputItemProgress matchLoadedItem(InputItemProgress inputItem) {
        for (InputItemProgress item : this.loadedItems) {
            if (item.equals(inputItem)) {
                return item;
            }
        }
        return null;
    }

    public InputItemProgress matchLoadingItem(InputItemProgress inputItem) {
        if (this.loadingItem != null && this.loadingItem.equals(inputItem)) {
            return this.loadingItem;
        }
        return null;
    }

    public void addLoadedItem(InputItemProgress inputItemProgress) {
        this.loadedItems.add(inputItemProgress);
    }

    public void addLoadingItem(InputItemProgress inputItemProgress) {
        if (this.loadingItem != null) {
            this.loadingItems.add(this.loadingItem);
        }
        this.loadingItem = inputItemProgress;
    }

    public long loadingOffset() {
        return this.loadingItem == null ? 0L : this.loadingItem.offset();
    }

    public void markLoaded(boolean markAll) {
        if (!this.loadingItems.isEmpty()) {
            this.loadedItems.addAll(this.loadingItems);
            this.loadingItems.clear();
        }
        if (markAll && this.loadingItem != null) {
            this.loadedItems.add(this.loadingItem);
            this.loadingItem = null;
        }
    }

    public void confirmOffset() {
        for (InputItemProgress item : this.loadingItems) {
            item.confirmOffset();
        }
        if (this.loadingItem != null) {
            this.loadingItem.confirmOffset();
        }
    }
}
