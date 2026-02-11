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

import org.apache.hugegraph.options.HubbleOptions;

import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxyServletConfiguration{
    @Autowired
    private HugeConfig config;

    /**
     * Only register the proxy servlet when proxy.servlet_url is configured.
     * When not configured, this bean won't be created, preventing the proxy
     * servlet from intercepting all requests on root path.
     */
    @Bean
    @ConditionalOnProperty(name = "proxy.servlet_url", matchIfMissing = false)
    public ServletRegistrationBean servletRegistrationBean(){
        String servletUrl = config.get(HubbleOptions.PROXY_SERVLET_URL);
        String targetUrl = config.get(HubbleOptions.PROXY_TARGET_URL);
        
        // Additional safety check
        if (servletUrl == null || servletUrl.isEmpty()) {
            return null;
        }
        
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new IngestionProxyServlet(),
                servletUrl);
        servletRegistrationBean.addInitParameter("targetUri", targetUrl);
        servletRegistrationBean.addInitParameter(ProxyServlet.P_LOG, "true");
        return servletRegistrationBean;
    }
}
