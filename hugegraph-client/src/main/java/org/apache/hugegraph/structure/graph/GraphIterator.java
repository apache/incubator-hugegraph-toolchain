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

package org.apache.hugegraph.structure.graph;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.util.E;

public class GraphIterator<T extends GraphElement> implements Iterator<T> {

    private final GraphManager graphManager;
    private final int sizePerPage;
    private final Function<String, Pageable<T>> pageFetcher;
    private List<T> results;
    private String page;
    private int cursor;
    private boolean finished;

    public GraphIterator(final GraphManager graphManager, final int sizePerPage,
                         final Function<String, Pageable<T>> pageFetcher) {
        E.checkNotNull(graphManager, "Graph manager");
        E.checkNotNull(pageFetcher, "Page fetcher");
        this.graphManager = graphManager;
        this.sizePerPage = sizePerPage;
        this.pageFetcher = pageFetcher;
        this.results = null;
        this.page = "";
        this.cursor = 0;
        this.finished = false;
    }

    @Override
    public boolean hasNext() {
        if (this.results == null || this.cursor >= this.results.size()) {
            this.fetch();
        }
        assert this.results != null;
        return this.cursor < this.results.size();
    }

    private void fetch() {
        if (this.finished) {
            return;
        }
        Pageable<T> pageable = this.pageFetcher.apply(this.page);
        this.results = pageable.results();
        this.page = pageable.page();
        this.cursor = 0;
        E.checkState(this.results.size() <= this.sizePerPage,
                     "Server returned unexpected results: %s > %s",
                     this.results.size(), this.sizePerPage);
        if (this.results.size() < this.sizePerPage || this.page == null) {
            this.finished = true;
        }
    }

    @Override
    public T next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }

        T elem = this.results.get(this.cursor++);
        E.checkState(elem != null,
                     "The server data is invalid, some records are null");
        elem.attachManager(this.graphManager);
        return elem;
    }
}
