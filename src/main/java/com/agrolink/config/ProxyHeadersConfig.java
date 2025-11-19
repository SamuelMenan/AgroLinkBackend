package com.agrolink.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
public class ProxyHeadersConfig {

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        // Honor X-Forwarded-* headers from proxies (Render) for correct scheme/host
        return new ForwardedHeaderFilter();
    }
}
