package com.abhishek.distributedjobqueue.execution.service;

import com.abhishek.distributedjobqueue.job.entity.Job;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JobExecutor {

    @Value("${server.port}")
    private String port;
    public void execute(@NonNull Job job) {

        log.info("[Worker:{}] Starting execution for job {}", port, job.getId());

        try {
            // simulate processing time
            Thread.sleep(2000);

            log.info("[Worker:{}] Processing job {} (attempt={})",
                    port,
                    job.getId(),
                    job.getAttemptCount());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Job execution interrupted for jobId: " + job.getId(), e);
        }

        log.info("[Worker:{}] Completed job {}", port, job.getId());
    }
}