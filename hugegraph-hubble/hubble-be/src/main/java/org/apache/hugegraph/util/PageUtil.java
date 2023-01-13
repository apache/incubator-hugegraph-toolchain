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

package org.apache.hugegraph.util;

import java.util.Collections;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;

public final class PageUtil {

    public static <T> IPage<T> page(List<T> entities, int pageNo, int pageSize) {
        // Regard page no < 1 as 1
        int current = pageNo > 1 ? pageNo : 1;
        int pages;
        List<T> records;
        if (pageSize > 0) {
            List<List<T>> subEntities = Lists.partition(entities, pageSize);
            pages = subEntities.size();
            if (current <= subEntities.size()) {
                records = subEntities.get(current - 1);
            } else {
                records = Collections.emptyList();
            }
        } else {
            pages = 0;
            // Return all entities when page size is negative
            records = pageSize < 0 ? entities : Collections.emptyList();
        }

        Page<T> page = new Page<>(current, pageSize, entities.size(), true);
        page.setRecords(records);
        page.setOrders(Collections.emptyList());
        page.setPages(pages);
        return page;
    }
}
