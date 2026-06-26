package com.abhishek.distributedjobqueue.worker;

import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.enums.JobStatus;
import com.abhishek.distributedjobqueue.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JobRecoveryScheduler {
    private final JobRepository jobRepository;
    @Value("${server.port}")
    private String port;

    @Scheduled(fixedDelay = 60000)
    public void recoverStuckJobs() {
        log.info("[Recovery:{}] Looking for stuck jobs...", port);

        List<Job> stuckJobs = jobRepository.findStuckJobs(JobStatus.RUNNING, LocalDateTime.now().minusMinutes(5));

        for (Job job: stuckJobs) {
            log.warn("[Recovery:{}] Recovering stuck job {}", port, job.getId());
            job.setAttemptCount(job.getAttemptCount() + 1);

            if (job.getAttemptCount() >= job.getMaxAttempts()) {
                job.setStatus(JobStatus.FAILED);
            }
            else {
                job.setStatus(JobStatus.PENDING);
            }

            jobRepository.save(job);
        }
    }
}
