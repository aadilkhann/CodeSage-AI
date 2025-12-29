package com.codesage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * Configuration for WebClient
 * Used for making HTTP calls to external APIs (GitHub, AI Service)
 */
@Configuration
public class WebClientConfig {

    /**
     * Configure WebClient.Builder bean
     * 
     * Features:
     * - Connection pooling
     * - Timeouts configured
     * - Suitable for high-volume API calls
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        // Configure connection pool
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(100)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .build();

        // Configure HTTP client
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(Duration.ofSeconds(30));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
