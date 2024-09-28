package org.apache.hugegraph.entity.graphs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphCloneEntity {
    @JsonProperty("graphspace")
    public String graphSpace;
    @JsonProperty("name")
    public String name;
    @JsonProperty("nickname")
    public String nickname;
    @JsonProperty("load_data")
    public int loadData = 0;

    public Map<String, Object> convertMap(String graphSpace, String graph) {
        Map<String, Object> params = new HashMap<>(4);

        String name = (this.name == null) ? graph : this.name;
        String nickname = (this.nickname == null) ? graph : this.nickname;
        String space = (this.graphSpace == null) ? graphSpace : this.graphSpace;

        Map<String, Object> configs = new HashMap<>();
        configs.put("backend", "hstore");
        configs.put("serializer", "binary");
        configs.put("name", name);
        configs.put("nickname", nickname);
        configs.put("search.text_analyzer", "jieba");
        configs.put("search.text_analyzer_mode", "INDEX");

        params.put("configs", configs);
        params.put("create", true);
        params.put("init_schema", true);
        params.put("graphspace", space);
        params.put("load_data", (boolean) (this.loadData == 1));
        return params;
    }
}
