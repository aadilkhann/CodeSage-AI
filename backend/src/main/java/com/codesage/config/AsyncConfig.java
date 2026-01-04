package com.codesage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration
 * 
 * Configures thread pool for async analysis processing
 * Prevents blocking the main request threads
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool executor for analysis tasks
     * 
     * Configuration:
     * - Core pool size: 5 threads (handles 5 concurrent analyses)
     * - Max pool size: 10 threads (can scale up to 10)
     * - Queue capacity: 25 (max 25 pending analyses)
     * 
     * Naming: Helps identify analysis threads in logs
     */
    @Bean(name = "analysisExecutor")
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("analysis-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
