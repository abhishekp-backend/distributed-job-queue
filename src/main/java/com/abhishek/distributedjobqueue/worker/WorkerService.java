package com.abhishek.distributedjobqueue.worker;

import com.abhishek.distributedjobqueue.execution.JobExecutor;
import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.service.JobService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Getter
@Setter
@ConfigurationProperties(prefix = "worker")
public class WorkerService {

    private final JobExecutor jobExecutor;
    private final JobService jobService;

    @Value("${server.port}")
    private String port;

    private int batchSize;

    public boolean processNextJob() {

        List<Job> jobs = jobService.claimPendingJobs();

        if (jobs.isEmpty()) {
            return false;
        }

        log.info("Found {} jobs.", batchSize);

        for (Job job : jobs) {

            log.info("[Worker:{}] Found job {}", port, job.getId());

            try {
                jobExecutor.execute(job);

                jobService.markCompleted(job.getId());

                log.info("[Worker:{}] Completed job {}", port, job.getId());

            } catch (Exception e) {

                log.error("[Worker:{}] Job {} failed", port, job.getId(), e);

                jobService.retryOrFail(job.getId());
            }
        }

        return true;
    }
}