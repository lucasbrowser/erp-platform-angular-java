package com.erp.core.platform.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PaginationConfiguration {
    @Bean
    PageableHandlerMethodArgumentResolverCustomizer paginationLimits() {
        return resolver -> resolver.setMaxPageSize(100);
    }
}
