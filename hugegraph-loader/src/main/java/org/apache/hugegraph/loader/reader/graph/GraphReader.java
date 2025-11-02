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

package org.apache.hugegraph.loader.reader.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.loader.exception.InitException;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.reader.AbstractReader;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.loader.source.graph.GraphSource;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.loader.source.InputSource;

public class GraphReader extends AbstractReader {
    private final GraphSource source;
    private HugeClient sourceClient;

    private GraphFetcher fetcher;

    private Map<String, List<String>> selectedVertices = new HashMap<>();
    private Map<String, Map<String, Object>> selectedVerticesConds =
            new HashMap<>();
    private Map<String, List<String>> ignoredVertices = new HashMap<>();
    private Map<String, List<String>> selectedEdges = new HashMap<>();
    private Map<String, Map<String, Object>> selectedEdgesConds =
            new HashMap<>();
    private Map<String, List<String>> ignoredEdges = new HashMap<>();

    private Iterator<String> selectedVertexLabels;
    private Iterator<String> selectedEdgeLabels;

    public GraphReader(GraphSource source) {
        this.source = source;
    }

    public GraphReader newGraphReader(InputSource source,
                                      Map<String, List<String>> selectedVertices,
                                      Map<String, Map<String, Object>> selectedVerticesConds,
                                      Map<String, List<String>> ignoredVertices,
                                      Map<String, List<String>> selectedEdges,
                                      Map<String, Map<String, Object>> selectedEdgesConds,
                                      Map<String, List<String>> ignoredEdges) {

        GraphReader reader = new GraphReader((GraphSource) source);

        reader.selectedVertices = selectedVertices;
        reader.selectedVerticesConds = selectedVerticesConds;
        reader.ignoredVertices = ignoredVertices;
        reader.selectedEdges = selectedEdges;
        reader.selectedEdgesConds = selectedEdgesConds;
        reader.ignoredEdges = ignoredEdges;

        reader.selectedVertexLabels = selectedVertices.keySet().iterator();
        reader.selectedEdgeLabels = selectedEdges.keySet().iterator();

        reader.newNextGraphFetcher();

        return reader;
    }

    @Override
    public void init(LoadContext context,
                     InputStruct struct) throws InitException {

        this.progress(context, struct);

        // Create HugeClient for readding graph element;
        this.sourceClient = this.source.createHugeClient();
        this.sourceClient.assignGraph(this.source.getGraphSpace(),
                                      this.source.getGraph());

        // Do with Vertex
        // 1. Get All Selected Vertex
        if (this.source.getSelectedVertices() != null) {
            for (GraphSource.SelectedLabelDes selected :
                    this.source.getSelectedVertices()) {

                selectedVertices.put(selected.getLabel(), null);
                if (selected.getQuery() != null && selected.getQuery().size() > 0) {
                    selectedVerticesConds.put(selected.getLabel(),
                                              selected.getQuery());
                }

                // generate ignored properties
                if (selected.getProperties() != null) {
                    VertexLabel vl =
                            this.sourceClient.schema().getVertexLabel(selected.getLabel());
                    Set<String> properties = vl.properties();
                    properties.removeAll(selected.getProperties());
                    ignoredVertices.put(selected.getLabel(),
                                        new ArrayList<>(properties));
                }
            }
        } else {
            for (VertexLabel label : this.sourceClient.schema()
                                                      .getVertexLabels()) {
                selectedVertices.put(label.name(), null);
            }
        }

        // 2. Remove ingnored vertex && vertex.properties
        if (this.source.getIgnoredVertices() != null) {
            for (GraphSource.IgnoredLabelDes ignored :
                    this.source.getIgnoredVertices()) {
                if (ignored.getProperties() == null) {
                    this.selectedVertices.remove(ignored.getLabel());
                } else {
                    this.ignoredVertices.put(ignored.getLabel(),
                                             ignored.getProperties());
                }
            }
        }

        // Do with edges
        // 1. Get All Selected Edges
        if (this.source.getSelectedEdges() != null) {
            for (GraphSource.SelectedLabelDes selected :
                    this.source.getSelectedEdges()) {
                selectedEdges.put(selected.getLabel(), null);
                if (selected.getQuery() != null && selected.getQuery().size() > 0) {
                    selectedEdgesConds.put(selected.getLabel(),
                                           selected.getQuery());
                }

                // generate ignored properties
                if (selected.getProperties() != null) {
                    EdgeLabel vl =
                            this.sourceClient.schema()
                                             .getEdgeLabel(selected.getLabel());
                    Set<String> properties = vl.properties();
                    properties.removeAll(selected.getProperties());

                    ignoredEdges.put(selected.getLabel(),
                                     new ArrayList(properties));
                }
            }
        } else {
            for (EdgeLabel label : this.sourceClient.schema()
                                                    .getEdgeLabels()) {
                selectedEdges.put(label.name(), null);
            }
        }

        // 2. Remove ignored Edge
        if (this.source.getIgnoredEdges() != null) {
            for (GraphSource.IgnoredLabelDes ignored :
                    this.source.getIgnoredEdges()) {
                if (CollectionUtils.isEmpty(ignored.getProperties())) {
                    this.selectedEdges.remove(ignored.getLabel());
                } else {
                    this.ignoredEdges.put(ignored.getLabel(),
                                          ignored.getProperties());
                }
            }
        }

        this.selectedVertexLabels = selectedVertices.keySet().iterator();
        this.selectedEdgeLabels = selectedEdges.keySet().iterator();

        this.newNextGraphFetcher();
    }

    @Override
    public void confirmOffset() {
        // Do Nothing
    }

    @Override
    public void close() {
        if (this.sourceClient != null) {
            this.sourceClient.close();
        }
    }

    @Override
    public boolean multiReaders() {
        return false;
    }

    @Override
    public boolean hasNext() {
        if (this.fetcher == null) {
            return false;
        }
        if (this.fetcher.hasNext()) {
            return true;
        } else {
            newNextGraphFetcher();

            if (fetcher != null) {
                return this.fetcher.hasNext();
            }
        }

        return false;
    }

    private void newNextGraphFetcher() {
        if (this.selectedVertexLabels.hasNext()) {
            String label = this.selectedVertexLabels.next();
            this.fetcher = new GraphFetcher(this.sourceClient, label,
                                            this.selectedVerticesConds.get(label),
                                            this.source.getBatchSize(), true,
                                            ignoredVertices.get(label));

        } else if (this.selectedEdgeLabels.hasNext()) {
            String label = this.selectedEdgeLabels.next();
            this.fetcher = new GraphFetcher(this.sourceClient, label,
                                            this.selectedEdgesConds.get(label),
                                            this.source.getBatchSize(), false,
                                            ignoredEdges.get(label));
        } else {
            this.fetcher = null;
        }
    }

    @Override
    public Line next() {
        GraphElement element = this.fetcher.next();

        return new Line("", new String[]{"fake"}, new Object[]{element});
    }
}
