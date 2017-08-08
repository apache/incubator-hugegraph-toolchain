package com.baidu.hugegraph.structure.version;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Versions {

    @JsonProperty
    private Map<String, String> versions;

    public String get(String name) {
        return this.versions.get(name);
    }
}
