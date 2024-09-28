package org.apache.hugegraph.config;

import org.apache.hugegraph.options.HubbleOptions;

import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxyServletConfiguration{
    @Autowired
    private HugeConfig config;

    @Bean
    public ServletRegistrationBean servletRegistrationBean(){
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new IngestionProxyServlet(),
                config.get(HubbleOptions.PROXY_SERVLET_URL));
        servletRegistrationBean.addInitParameter("targetUri", config.get(HubbleOptions.PROXY_TARGET_URL));
        servletRegistrationBean.addInitParameter(ProxyServlet.P_LOG, "true");
        return servletRegistrationBean;
    }
}
