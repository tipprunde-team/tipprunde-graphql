package com.mosbach.tipprunde.graphql.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

// WebClient für den Core-Service: nicht blockierend, mit Zeitlimit
@Configuration
public class CoreClientConfig {

    @Bean
    public WebClient coreWebClient(@Value("${app.core.base-url}") String baseUrl,
            @Value("${app.core.timeout-seconds}") long timeoutSeconds) {
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(timeoutSeconds));
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
