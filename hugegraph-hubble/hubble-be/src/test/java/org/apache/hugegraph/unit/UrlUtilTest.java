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

import org.junit.Test;

import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.util.UrlUtil;

public class UrlUtilTest {

    @Test
    public void testParseHostIgnoresPathAndQuery() {
        UrlUtil.Host host = UrlUtil.parseHost(
                "http://127.0.0.1:8080/graphs/hugegraph?debug=true");

        Assert.assertEquals("http", host.getScheme());
        Assert.assertEquals("127.0.0.1", host.getHost());
        Assert.assertEquals(8080, host.getPort());
    }

    @Test
    public void testParseHostUsesSchemeDefaultPort() {
        UrlUtil.Host http = UrlUtil.parseHost("http://localhost");
        Assert.assertEquals("http", http.getScheme());
        Assert.assertEquals("localhost", http.getHost());
        Assert.assertEquals(80, http.getPort());

        UrlUtil.Host https = UrlUtil.parseHost("https://example.com/base");
        Assert.assertEquals("https", https.getScheme());
        Assert.assertEquals("example.com", https.getHost());
        Assert.assertEquals(443, https.getPort());
    }

    @Test
    public void testParseHostHandlesIpv6Literal() {
        UrlUtil.Host host = UrlUtil.parseHost("http://[::1]:8080/path");

        Assert.assertEquals("http", host.getScheme());
        Assert.assertEquals("::1", host.getHost());
        Assert.assertEquals(8080, host.getPort());
    }

    @Test
    public void testParseHostKeepsHostPortWithoutScheme() {
        UrlUtil.Host host = UrlUtil.parseHost("127.0.0.1:8080");

        Assert.assertEquals(null, host.getScheme());
        Assert.assertEquals("127.0.0.1", host.getHost());
        Assert.assertEquals(8080, host.getPort());
    }

    @Test
    public void testParseHostWithoutSchemeAllowsAbsoluteUrlInPath() {
        UrlUtil.Host host = UrlUtil.parseHost(
                "127.0.0.1:8080/path?next=http://example.com");

        Assert.assertEquals(null, host.getScheme());
        Assert.assertEquals("127.0.0.1", host.getHost());
        Assert.assertEquals(8080, host.getPort());
    }

    @Test
    public void testParseHostRejectsEmptyExplicitPort() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            UrlUtil.parseHost("http://localhost:");
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            UrlUtil.parseHost("https://localhost:/path");
        });
    }
}
