package com.abhishek.distributedjobqueue.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Semaphore;

@Configuration
public class WorkerConfig {

    @Value("${worker.threads:4}")
    private int workerThreads;

    @Bean(name = "workerTaskExecutor")
    public ThreadPoolTaskExecutor workerTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(workerThreads);
        executor.setMaxPoolSize(workerThreads);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("job-worker-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        return executor;
    }

    @Bean
    public Semaphore workerSlots() {
        return new Semaphore(workerThreads);
    }
}