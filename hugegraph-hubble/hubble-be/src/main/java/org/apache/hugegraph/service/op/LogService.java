/*
 *
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

package org.apache.hugegraph.service.op;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.json.JsonData;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.entity.op.LogEntity;
import org.apache.hugegraph.util.PageUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LogService extends ESService {

    protected final String allIndexName = "*";

    private final String logSortKey = "@timestamp";
    private final String sortOrder = "Asc";

    public IPage<LogEntity> queryPage(LogReq logReq) throws IOException {

        List<LogEntity> logs = new ArrayList<>();

        List<String> indexes = new ArrayList<>();
        // services
        List<String> services = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(logReq.services)) {
            services.addAll(logReq.services);
        } else {
            services.addAll(listServices());
        }
        services.forEach(s -> indexes.add(s + "-*"));

        List<Query> querys = buildQuery(logReq);

        FieldSort sort =
                SortOptionsBuilders.field().field(logSortKey)
                                   .order(SortOrder.valueOf(sortOrder)).build();
        SortOptions sortKeyOption =
                new SortOptions.Builder().field(sort).build();

        SearchResponse<Map> search = esClient().search((s) ->
            s.index(indexes).from(Math.max(logReq.pageNo - 1, 0) * logReq.pageSize)
             .size(logReq.pageSize)
             .query(q -> q.bool( boolQuery ->
                        boolQuery.must(querys)
                    )
             ).sort(sortKeyOption), Map.class);

        for (Hit<Map> hit: search.hits().hits()) {
            logs.add(LogEntity.fromMap((Map<String, Object>) hit.source()));
        }

        return PageUtil.newPage(logs, logReq.pageNo, logReq.pageSize,
                                (int)(search.hits().total().value()));
    }

    public List<LogEntity> export(LogReq logReq) throws IOException {
        List<LogEntity> logs = new ArrayList<>();

        List<String> indexes = new ArrayList<>();
        // services
        List<String> services = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(logReq.services)) {
            services.addAll(logReq.services);
        } else {
            services.addAll(listServices());
        }
        services.forEach(s -> indexes.add(s + "-*"));

        List<Query> querys = buildQuery(logReq);

        FieldSort sort =
                SortOptionsBuilders.field().field(logSortKey)
                                   .order(SortOrder.valueOf(sortOrder)).build();
        SortOptions sortKeyOption =
                new SortOptions.Builder().field(sort).build();

        int batchSize = maxResultWindow();
        int countLimit = exportCountLimit();

        int times = (int) Math.ceil((double) countLimit / batchSize);

        for (int i = 0; i < times; i++) {
            int start = i * batchSize;

            SearchResponse<Map> search = esClient().search((s) ->
               s.index(indexes).from(start).size(batchSize)
                .query(q -> q.bool( boolQuery -> boolQuery.must(querys))
                ).sort(sortKeyOption), Map.class);

            for (Hit<Map> hit: search.hits().hits()) {
                logs.add(LogEntity.fromMap((Map<String, Object>) hit.source()));
            }

            int resultCount = (int) (search.hits().total().value());
            if (resultCount < batchSize) {
                break;
            }
        }

        return logs;
    }

    protected  List<Query>  buildQuery(LogReq logReq) {
        // 根据Query信息，生成ES的query

        List<Query> querys = new ArrayList<>();
        // start_datetime, end_datetime
        if(logReq.startDatetime != null || logReq.endDatetime != null) {
            Query.Builder builder = new Query.Builder();
            // builder.range(e->e.field())
            RangeQuery.Builder rBuilder = new RangeQuery.Builder();
            rBuilder = rBuilder.field("@timestamp");
            if(logReq.startDatetime != null) {
                rBuilder = rBuilder.gte(JsonData.of(logReq.startDatetime));
            }
            if(logReq.endDatetime != null) {
                rBuilder = rBuilder.lte(JsonData.of(logReq.endDatetime));
            }
            querys.add(builder.range(rBuilder.build()).build());
        }

        // query
        if(!StringUtils.isEmpty(logReq.query)) {
            Query.Builder builder = new Query.Builder();

            MatchQuery.Builder mBuilder = new MatchQuery.Builder();
            mBuilder.field("message").query(FieldValue.of(logReq.query));

            querys.add(builder.match(mBuilder.build()).build());
        }

        // hosts
        if (CollectionUtils.isNotEmpty(logReq.hosts)) {
            Query.Builder builder = new Query.Builder();

            TermsQuery.Builder tBuilder = new TermsQuery.Builder();
            TermsQueryField.Builder fieldBuilder = new TermsQueryField.Builder();
            fieldBuilder.value(logReq.hosts.stream().map(FieldValue::of)
                                           .collect(Collectors.toList()));
            tBuilder.field("host.hostname.keyword").terms(fieldBuilder.build());

            querys.add(builder.terms(tBuilder.build()).build());
        }

        // Level
        if (StringUtils.isNotEmpty(logReq.level)) {
            int levelIndex = ArrayUtils.indexOf(LEVELS,
                                                logReq.level.toUpperCase());

            String[] retianLevels = Arrays.copyOfRange(LEVELS, 0,
                                                       levelIndex + 1);

            Query.Builder builder = new Query.Builder();

            TermsQuery.Builder tBuilder = new TermsQuery.Builder();
            TermsQueryField.Builder fieldBuilder = new TermsQueryField.Builder();
            fieldBuilder.value(Arrays.stream(retianLevels).map(FieldValue::of)
                                     .collect(Collectors.toList()));
            tBuilder.field("level.keyword").terms(fieldBuilder.build());

            querys.add(builder.terms(tBuilder.build()).build());
        }

        return querys;
    }

    @Cacheable(value = "ES_QUERY", key="#root.targetClass.name+':'+#root" +
            ".methodName")
    public synchronized List<String> listServices() throws IOException {
        Set<String> services = new HashSet<>();

        GetAliasResponse res = esClient().indices().getAlias();
        res.result().keySet().stream()
           // filter hidden index and audit index
           .filter(x -> !(x.startsWith(".") || x.contains(logAuditPattern())))
           .forEach(indexName -> services.add(indexName.split("-")[0]));

        return services.stream().sorted().collect(Collectors.toList());
    }

    @Cacheable(value = "ES_QUERY", key="#root.targetClass.name+':'+#root" +
            ".methodName")
    public List<String> listHosts() throws IOException {

        List<String> hosts = new ArrayList<>();

        final String hostField = "host.hostname.keyword";

        return esAggTerms(ImmutableList.of(allIndexName), hostField, 20);
    }

    protected List<String> esAggTerms(List<String> indexNames, String field,
                                      int top) throws IOException {
        String key = "field_key";
        Aggregation agg = Aggregation.of(
                a -> a.terms(v -> v.field(field).size(top)));

        // DO Request
        SearchResponse<Object> response
                = esClient().search((s) -> s.index(indexNames)
                                          .aggregations(key, agg),
                                  Object.class);

        // Get agg terms from response
        Buckets<StringTermsBucket> buckets = response.aggregations()
                                                     .get(key)
                                                     .sterms()
                                                     .buckets();

        return buckets.array().stream().map(b -> b.key())
                      .collect(Collectors.toList());

    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogReq {
        @JsonProperty("start_datetime")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd "
                + "HH:mm:ss", timezone = "GMT+8")
        public Date startDatetime;

        @JsonProperty("end_datetime")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd " +
                "HH:mm:ss", timezone = "GMT+8")
        public Date endDatetime;

        @JsonProperty("query")
        public String query;

        @JsonProperty("level")
        public String level;

        @JsonProperty("services")
        public List<String> services = new ArrayList<>();

        @JsonProperty("hosts")
        public List<String> hosts = new ArrayList<>();

        @JsonProperty("page_no")
        public int pageNo = 1;

        @JsonProperty("page_size")
        public int pageSize = 20;
    }
}
