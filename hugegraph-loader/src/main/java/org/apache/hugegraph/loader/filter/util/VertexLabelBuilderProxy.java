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

package org.apache.hugegraph.loader.filter.util;

import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.structure.schema.VertexLabel;

public class VertexLabelBuilderProxy implements VertexLabel.Builder {

    private VertexLabel.BuilderImpl builder;

    private ShortIdConfig config;
    public VertexLabelBuilderProxy(String name, SchemaManager manager, LoadOptions options) {
        this.builder = new VertexLabel.BuilderImpl(name, manager);
        for (ShortIdConfig config : options.shorterIDConfigs) {
            if (config.getVertexLabel().equals(name)) {
                this.config = config;
                break;
            }
        }
    }

    @Override
    public VertexLabel build() {
        return builder.build();
    }

    @Override
    public VertexLabel create() {
        return builder.create();
    }

    @Override
    public VertexLabel append() {
        return builder.append();
    }

    @Override
    public VertexLabel eliminate() {
        return builder.eliminate();
    }

    @Override
    public void remove() {
        builder.remove();
    }

    @Override
    public VertexLabel.Builder idStrategy(IdStrategy idStrategy) {
        builder.idStrategy(idStrategy);
        return this;
    }

    @Override
    public VertexLabel.Builder useAutomaticId() {
        builder.useAutomaticId();
        return this;
    }

    @Override
    public VertexLabel.Builder usePrimaryKeyId() {
        if (config != null) {
            builder.properties(config.getIdFieldName());
            builder.nullableKeys(config.getIdFieldName());
        } else {
            builder.usePrimaryKeyId();
        }
        return this;
    }

    @Override
    public VertexLabel.Builder useCustomizeStringId() {
        builder.useCustomizeStringId();
        if (config != null) {
            builder.properties(config.getIdFieldName());
            builder.nullableKeys(config.getIdFieldName());
        }
        return this;
    }

    @Override
    public VertexLabel.Builder useCustomizeNumberId() {
        builder.useCustomizeNumberId();
        if (config != null) {
            builder.properties(config.getIdFieldName());
            builder.nullableKeys(config.getIdFieldName());
        }
        return this;
    }

    @Override
    public VertexLabel.Builder useCustomizeUuidId() {
        builder.useCustomizeUuidId();
        if (config != null) {
            builder.properties(config.getIdFieldName());
            builder.nullableKeys(config.getIdFieldName());
        }
        return this;
    }

    @Override
    public VertexLabel.Builder properties(String... properties) {
        builder.properties(properties);
        return this;
    }

    @Override
    public VertexLabel.Builder primaryKeys(String... keys) {
        if (config != null) {
            /* only support one primaryKey */
            config.setPrimaryKeyField(keys[0]);
            builder.useCustomizeNumberId();
            builder.properties(config.getIdFieldName());
            builder.nullableKeys(config.getIdFieldName());
        } else {
            builder.primaryKeys(keys);
        }

        return this;
    }

    @Override
    public VertexLabel.Builder nullableKeys(String... keys) {
        builder.nullableKeys(keys);
        return this;
    }

    @Override
    public VertexLabel.Builder ttl(long ttl) {
        builder.ttl(ttl);
        return this;
    }

    @Override
    public VertexLabel.Builder ttlStartTime(String ttlStartTime) {
        builder.ttlStartTime(ttlStartTime);
        return this;
    }

    @Override
    public VertexLabel.Builder enableLabelIndex(boolean enable) {
        builder.enableLabelIndex(enable);
        return this;
    }

    @Override
    public VertexLabel.Builder userdata(String key, Object val) {
        builder.userdata(key, val);
        return this;
    }

    @Override
    public VertexLabel.Builder ifNotExist() {
        builder.ifNotExist();
        return this;
    }

    @Override
    public VertexLabel.Builder id(long id) {
        builder.id(id);
        return this;
    }
}
