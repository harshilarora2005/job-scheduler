package com.jobr.api_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * dashboard-service serves a live React dashboard (static/index.html) on a different port
 * (8083) that POSTs test jobs straight to this service's /jobs endpoint from the browser.
 * That's a cross-origin request, so it needs an explicit CORS allowance - without this,
 * the browser blocks the response before JS ever sees it.
 *
 * Using an explicit CorsFilter bean rather than WebMvcConfigurer.addCorsMappings - the
 * filter runs earlier in the chain (before DispatcherServlet/HandlerMapping), so it
 * reliably handles the browser's OPTIONS preflight request regardless of how MVC's own
 * CORS-mapping registration interacts with other config in this project (which was
 * unreliable here - see the WebMvcConfigurer version this replaced).
 *
 * Scoped to localhost dev ports only, not a wildcard - this is a load-test/demo tool, not a
 * public API, and there's no reason to open it up further than that.
 */
@Configuration
public class WebConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:8083"));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/jobs", config);

        return new CorsFilter(source);
    }
}
