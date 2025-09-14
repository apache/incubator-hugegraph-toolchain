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

package org.apache.hugegraph.loader.filter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;

public class ElementLimitFilter implements ElementParser {

    private static final int LRU_CAPACITY = 10 * 10000;

    private final long limit;
    private Map<Object, AtomicLong> records;
    private LruCounter<Object> counter;

    public ElementLimitFilter(long limit) {
        this.limit = limit;
        this.records = new ConcurrentHashMap<>();
        this.counter = new LruCounter<>(LRU_CAPACITY, true);
    }

    @Override
    public boolean parse(GraphElement element) {
        if (element instanceof Vertex) {
            return true;
        }
        Edge edge = (Edge) element;
        records.computeIfAbsent(edge.sourceId(), k -> new AtomicLong(1));
        AtomicLong count = records.computeIfPresent(edge.sourceId(), (k, v) -> {
            v.addAndGet(1);
            return v;
        });
        return counter.addAndGet(edge.sourceId()) <= limit
                && counter.addAndGet(edge.targetId()) <= limit;
    }

    class LruCounter<K> {
        /*TODO: optimize V as a linkedlist entry -> O(1) remove&add */
        private Map<K, AtomicLong> map;
        private Queue<K> lastUsedQueue;
        private final int capacity;

        public LruCounter(int capacity, boolean concurrent) {
            this.capacity = capacity;
            if (concurrent) {
                map = new ConcurrentHashMap<>(capacity);
                lastUsedQueue = new ConcurrentLinkedQueue<>();
            } else {
                map = new HashMap<>();
                lastUsedQueue = new LinkedList();
            }
        }

        long addAndGet(K key) {
            Number value = map.get(key);
            if (value == null) {
                value = putNewValue(key);
            }
            refreshKey(key);
            return value.longValue();
        }

        private synchronized void refreshKey(K key) {
            lastUsedQueue.remove(key);
            lastUsedQueue.add(key);
        }

        private synchronized AtomicLong putNewValue(K key) {
            if (!map.containsKey(key)) {
                if (map.size() >= capacity) {
                    K keyToRemove = lastUsedQueue.poll();
                    map.remove(keyToRemove);
                }
                AtomicLong value = new AtomicLong(1);
                map.put(key, value);
                lastUsedQueue.add(key);
                return value;
            }
            return map.get(key);
        }
    }

}
