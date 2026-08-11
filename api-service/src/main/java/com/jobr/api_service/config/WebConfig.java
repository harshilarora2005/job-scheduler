package com.jobr.api_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * dashboard-service serves a live React dashboard (static/index.html) on a different port
 * (8083) that POSTs test jobs straight to this service's /jobs endpoint from the browser.
 * That's a cross-origin request, so it needs an explicit CORS allowance - without this,
 * the browser blocks the response before JS ever sees it (fails silently as a network error).
 *
 * Scoped to localhost dev ports only, not a wildcard - this is a load-test/demo tool, not a
 * public API, and there's no reason to open it up further than that.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/jobs")
                .allowedOrigins("http://localhost:8083")
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}
