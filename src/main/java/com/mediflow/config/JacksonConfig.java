package com.mediflow.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Teaches Jackson about Hibernate proxies so lazily-loaded references never
 * break JSON serialization ("no Session" / ByteBuddy errors).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                // ISO-8601 strings ("2026-10-03T15:02:41") instead of [2026,10,3,...] arrays
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // modulesToInstall ADDS to Spring's auto-detected modules (JSR-310 dates, etc.)
                .modulesToInstall(new Hibernate6Module()
                        .configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, true));
    }
}
