package com.baidu.hugegraph.loader.reader.file;

import java.util.Set;

import com.baidu.hugegraph.loader.progress.InputProgress;
import com.baidu.hugegraph.loader.reader.Readable;
import com.baidu.hugegraph.util.InsertionOrderUtil;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReadableProgress implements InputProgress {

    @JsonProperty("loaded_items")
    private Set<String> loadedItems;
    @JsonProperty("loading_item")
    private LoadingItem loadingItem;

    public ReadableProgress() {
        this.loadedItems = InsertionOrderUtil.newSet();
        this.loadingItem = new LoadingItem();
    }

    public Set<String> loadedItems() {
        return this.loadedItems;
    }

    public void loadedItem(Readable readable) {
        this.loadedItems.add(readable.uniqueKey());
        if (readable.uniqueKey().equals(this.loadingItem.name)) {
            this.loadingItem = new LoadingItem();
        }
    }

    public LoadingItem loadingItem() {
        return this.loadingItem;
    }

    public void loadingItem(Readable readable) {
        if (!readable.uniqueKey().equals(this.loadingItem.name)) {
            this.loadingItem.offset = 0;
        }
        this.loadingItem.name = readable.uniqueKey();
    }

    @Override
    public void increaseOffset(int size) {
        this.loadingItem.offset += size;
    }

    public static class LoadingItem {

        @JsonProperty("unique_key")
        private String name;
        @JsonProperty("offset")
        private long offset;

        public LoadingItem() {
            this.name = null;
            this.offset = -1;
        }

        public String name() {
            return name;
        }

        public long offset() {
            return offset;
        }
    }
}
