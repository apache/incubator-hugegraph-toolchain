package com.baidu.hugegraph.api.gremlin;

import java.util.HashMap;
import java.util.Map;

import com.baidu.hugegraph.driver.GremlinManager;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by liningrui on 2017/5/24.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GremlinRequest {

    // see org.apache.tinkerpop.gremlin.server.channel.HttpChannelizer
    public String gremlin;
    public Map<String, Object> bindings;
    public String language;
    public Map<String, String> aliases;

    public GremlinRequest(String gremlin) {
        this.gremlin = gremlin;
        this.bindings = new HashMap<>();
        this.language = "gremlin-groovy";
        this.aliases = new HashMap<>();
    }

    public static class Builder {
        private GremlinRequest request;
        private GremlinManager executor;

        public Builder(String gremlin, GremlinManager executor) {
            this.request = new GremlinRequest(gremlin);
            this.executor = executor;
        }

        public String execute() {
            return this.executor.execute(this.request);
        }

        public Builder binding(String key, String value) {
            this.request.bindings.put(key, value);
            return this;
        }

        public Builder language(String language) {
            this.request.language = language;
            return this;
        }

        public Builder alias(String key, String value) {
            this.request.aliases.put(key, value);
            return this;
        }
    }
}
