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

    public static final int MAX_PAGE_SIZE = 500;

    /*
     * Hard cap used instead of an unbounded query when the frontend
     * passes page_size=-1 (meaning "all records")
     */
    public static final int HARD_CAP = 10000;

    /**
     * Bound the requested page size: page_size=-1 is accepted from the
     * frontend but is turned into a hard cap instead of unbounded.
     */
    public static int boundedSize(int requested) {
        if (requested < -1) {
            throw new IllegalArgumentException("page_size must be >= -1");
        }
        return requested == -1 ? HARD_CAP : requested;
    }

    public static long boundedSize(long requested) {
        if (requested < -1L) {
            throw new IllegalArgumentException("page_size must be >= -1");
        }
        return requested == -1L ? HARD_CAP : requested;
    }

    public static void checkPage(int pageNo, int pageSize) {
        boolean invalidSize = pageSize != -1 &&
                              (pageSize < 1 || pageSize > MAX_PAGE_SIZE);
        if (pageNo < 1 || invalidSize) {
            throw new IllegalArgumentException(
                      "page_no must be >= 1 and page_size must be -1 or between 1 " +
                      "and " + MAX_PAGE_SIZE);
        }
    }

    public static void checkPositivePage(int pageNo, int pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                      "page_no must be >= 1 and page_size must be between 1 and " +
                      MAX_PAGE_SIZE);
        }
    }

    public static <T> IPage<T> page(List<T> entities, int pageNo, int pageSize) {
        checkPage(pageNo, pageSize);
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
            if (pageSize < 0) {
                records = entities;
                pageSize = entities.size();
            } else {
                records = Collections.emptyList();
            }
        }

        Page<T> page = new Page<>(current, pageSize, entities.size(), true);
        page.setRecords(records);
        page.setOrders(Collections.emptyList());
        page.setPages(pages);
        return page;
    }

    public static <T> IPage<T> newPage(List<T> records, int pageNo,
                                       int pageSize, int total) {
        checkPositivePage(pageNo, pageSize);
        int current = pageNo;
        int pages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;

        Page<T> page = new Page<>(current, pageSize, total, true);
        page.setRecords(records);
        page.setOrders(Collections.emptyList());
        page.setPages(pages);

        return page;
    }
}
