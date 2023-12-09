package org.apache.hugegraph.structure.traverser;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VEStepEntity {

    @JsonProperty("label")
    public String label;

    @JsonProperty("properties")
    public Map<String, Object> properties;

    protected VEStepEntity() {
        this.properties = new HashMap<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("VEStepEntity{label=%s,properties=%s}",
                             this.label, this.properties);
    }

    public static class Builder {
        protected VEStepEntity veStepEntity;

        private Builder() {
            this.veStepEntity = new VEStepEntity();
        }

        public VEStepEntity.Builder label(String label) {
            this.veStepEntity.label = label;
            return this;
        }

        public VEStepEntity.Builder properties(Map<String, Object> properties) {
            this.veStepEntity.properties = properties;
            return this;
        }

        public VEStepEntity.Builder properties(String key, Object value) {
            this.veStepEntity.properties.put(key, value);
            return this;
        }

        public VEStepEntity build() {
            return this.veStepEntity;
        }

    }
}