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

package org.apache.hugegraph.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final int DEFAULT_MAXSIZE = 1000;
    private static final int DEFAULT_TTL = 600;

    public enum Caches {

        // No used
        GREMLIN_QUERY;

        private int maxSize = DEFAULT_MAXSIZE;
        private int ttl = DEFAULT_TTL;

        Caches() {
        }

        Caches(int maxSize, int ttl) {
            this.maxSize = maxSize;
            this.ttl = ttl;
        }

        public int maxSize() {
            return this.maxSize;
        }

        public int ttl() {
            return this.ttl;
        }
    }

    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        List<CaffeineCache> caches = new ArrayList<>();
        for (Caches c : Caches.values()) {
            Cache<Object, Object> cache;
            cache = Caffeine.newBuilder()
                            .recordStats()
                            .maximumSize(c.maxSize())
                            .expireAfterWrite(c.ttl(), TimeUnit.SECONDS)
                            .build();
            caches.add(new CaffeineCache(c.name(), cache));
        }
        cacheManager.setCaches(caches);
        return cacheManager;
    }
}
