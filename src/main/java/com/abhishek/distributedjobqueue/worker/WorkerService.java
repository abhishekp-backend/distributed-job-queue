package com.abhishek.distributedjobqueue.worker;

import com.abhishek.distributedjobqueue.execution.JobExecutor;
import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkerService {

    private final JobExecutor jobExecutor;
    private final JobService jobService;

    @Value("${server.port}")
    private String port;

    public boolean processNextJob() {

        List<Job> jobs = jobService.claimPendingJobs();

        if (jobs.isEmpty()) {
            return false;
        }

        for (Job job : jobs) {

            jobService.markRunning(job.getId());

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