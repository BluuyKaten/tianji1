package com.tianji.learning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {

    @Bean("delayTaskExecutor")
    public ExecutorService delayTaskExecutor() {
        // 核心线程数 = CPU 核心数
        int coreSize = Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(coreSize);
    }
}
