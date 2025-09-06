package org.apache.hugegraph.loader.reader.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.hugegraph.util.Log;
import org.slf4j.Logger;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.structure.GraphElement;

public class GraphFetcher implements Iterator<GraphElement> {

    public static final Logger LOG = Log.logger(GraphFetcher.class);

    private final HugeClient client;
    private final String label;
    private final Map<String, Object> queryProperties;
    private final int batchSize;
    private final boolean isVertex;
    private final List<String> ignoredProperties;

    private int offset = 0;
    private boolean done = false;

    private Iterator<GraphElement> batchIter;

    public GraphFetcher(HugeClient client, String label,
                        Map<String, Object> queryProperties, int batchSize,
                        boolean isVertex, List<String> ignoredProerties) {
        this.client = client;
        this.label = label;
        this.queryProperties = queryProperties;
        this.batchSize = batchSize;
        this.isVertex = isVertex;
        this.ignoredProperties = ignoredProerties;

        this.offset = 0;
        this.done = false;
    }

    /**
     * 按照批次查询数据
     * @return 如数据为空，返回空数组
     */
    private List<GraphElement> queryBatch() {
        List<GraphElement> elements = new ArrayList<>();

        if (this.done) {
            return elements;
        }

        if (isVertex) {
            elements.addAll(this.client.graph().listVertices(this.label,
                                                             this.queryProperties, true,
                                                             this.offset, batchSize));
        } else {
            elements.addAll(this.client.graph().getEdges(null, null, this.label,
                                                         this.queryProperties, true,
                                                         this.offset, batchSize));
        }

        elements.stream().forEach(e -> this.ignoreProperties(e));

        // 判断当前fetch是否已经结束
        if (elements.size() < batchSize) {
            this.done = true;
        }

        this.offset += elements.size();

        return elements;
    }

    private void queryIfNecessary() {
        if (this.batchIter == null || !this.batchIter.hasNext()) {
            this.batchIter = queryBatch().iterator();
        }
    }

    @Override
    public boolean hasNext() {
        queryIfNecessary();

        return this.batchIter.hasNext();
    }

    @Override
    public GraphElement next() {
        queryIfNecessary();

        return this.batchIter.next();
    }

    private void ignoreProperties(GraphElement element) {
        if (element != null && !CollectionUtils.isEmpty(this.ignoredProperties)) {
            for (String property : this.ignoredProperties) {
                element.properties().remove(property);
            }
        }
    }
}
