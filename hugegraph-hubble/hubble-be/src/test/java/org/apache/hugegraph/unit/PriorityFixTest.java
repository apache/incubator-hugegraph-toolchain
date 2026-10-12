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
package org.apache.hugegraph.unit;

import java.util.Arrays;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.handler.CustomInterceptor;
import org.apache.hugegraph.util.PageUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;

public class PriorityFixTest {

    @Test
    public void testAfterCompletionClosesRequestClient() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HugeClient client = Mockito.mock(HugeClient.class);
        request.setAttribute("hugeClient", client);

        new CustomInterceptor().afterCompletion(request,
                                                new MockHttpServletResponse(),
                                                new Object(), null);

        Mockito.verify(client).close();
        Assert.assertNull(request.getAttribute("hugeClient"));
    }

    @Test
    public void testNewPageCalculatesPageCount() {
        IPage<Integer> page = PageUtil.newPage(Arrays.asList(3, 4), 2, 2, 5);

        Assert.assertEquals(3L, page.getPages());
        Assert.assertEquals(5L, page.getTotal());
        Assert.assertEquals(Arrays.asList(3, 4), page.getRecords());
    }

    @Test
    public void testPageKeepsLegacyAllPageSentinel() {
        IPage<Integer> page = PageUtil.page(Arrays.asList(1, 2, 3), 1, -1);

        Assert.assertEquals(Arrays.asList(1, 2, 3), page.getRecords());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewPageRejectsZeroPageSize() {
        PageUtil.newPage(Arrays.asList(), 1, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPositivePageRejectsAllPageSentinel() {
        PageUtil.checkPositivePage(1, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBoundedIntRejectsUnsupportedNegativePageSize() {
        PageUtil.boundedSize(-2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBoundedLongRejectsUnsupportedNegativePageSize() {
        PageUtil.boundedSize(-2L);
    }

    @Test(expected = ExternalException.class)
    public void testInterceptorRejectsOversizedPage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/list");
        request.setParameter("page_size", "501");

        new CustomInterceptor().preHandle(request,
                                          new MockHttpServletResponse(),
                                          new Object());
    }

    @Test(expected = ExternalException.class)
    public void testInterceptorRejectsUnsupportedNegativePageSize() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/list");
        request.setParameter("page_size", "-2");

        new CustomInterceptor().preHandle(request,
                                          new MockHttpServletResponse(),
                                          new Object());
    }

    @Test
    public void testInterceptorKeepsLegacyAllPageSentinel() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/list");
        request.setParameter("page_size", "-1");

        Assert.assertTrue(new CustomInterceptor().preHandle(
                          request, new MockHttpServletResponse(), new Object()));
    }
}
