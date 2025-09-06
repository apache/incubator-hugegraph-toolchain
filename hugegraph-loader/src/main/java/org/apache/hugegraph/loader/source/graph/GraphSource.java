package org.apache.hugegraph.loader.source.graph;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.util.E;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.factory.PDHugeClientFactory;
import org.apache.hugegraph.loader.source.AbstractSource;
import org.apache.hugegraph.loader.source.SourceType;
import org.apache.hugegraph.loader.source.file.FileSource;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GraphSource extends AbstractSource {
    @JsonProperty("pd-peers")
    private String pdPeers;

    @JsonProperty("meta-endpoints")
    private String metaEndPoints;

    @JsonProperty("cluster")
    private String cluster;

    @JsonProperty("graphspace")
    private String graphSpace;

    @JsonProperty("graph")
    private String graph;
    
    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("selected_vertices")
    private List<SeletedLabelDes> selectedVertices;

    @JsonProperty("ignored_vertices")
    private List<IgnoredLabelDes> ignoredVertices;

    @JsonProperty("selected_edges")
    private List<SeletedLabelDes> selectedEdges;

    @JsonProperty("ignored_edges")
    private List<IgnoredLabelDes> ignoredEdges;

    @JsonProperty("batch_size")
    private int batchSize = 500;

    @Override
    public SourceType type() {
        return SourceType.GRAPH;
    }

    @Override
    public void check() throws IllegalArgumentException {
        super.check();

        E.checkArgument(!StringUtils.isEmpty(this.graphSpace),
                        "graphspace of GraphInput must be not empty");

        E.checkArgument(!StringUtils.isEmpty(this.graph),
                        "graph of GraphInput must be not empty");
    }

    @Override
    public FileSource asFileSource() {
        FileSource source = new FileSource();
        source.header(this.header());
        source.charset(this.charset());
        source.listFormat(this.listFormat());

        return source;
    }

    @Data
    public static class SeletedLabelDes {
        @JsonProperty("query")
        private Map<String, Object> query;

        @JsonProperty("label")
        private String label;

        @JsonProperty("properties")
        private List<String> properties;
    }

    @Data
    public static class IgnoredLabelDes {
        @JsonProperty("label")
        private String label;

        @JsonProperty("properties")
        private List<String> properties;
    }

    public HugeClient createHugeClient() {
        PDHugeClientFactory factory = new PDHugeClientFactory(this.pdPeers);
        try {
            return factory.createAuthClient(cluster, graphSpace, graph, null,
                                            username, password);
        } finally {
            factory.close();
        }
    }
}
