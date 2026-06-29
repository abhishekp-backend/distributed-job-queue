package com.abhishek.distributedjobqueue.worker;

import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.enums.JobStatus;
import com.abhishek.distributedjobqueue.job.repository.JobRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.abhishek.distributedjobqueue.job.service.JobService;

import java.time.LocalDateTime;
import java.util.List;

@ConfigurationProperties(prefix = "recovery")
@Component
@Getter
@Setter
@Slf4j
@RequiredArgsConstructor
public class JobRecoveryScheduler {
    private final JobRepository jobRepository;
    private final JobService jobService;

    @Value("${server.port}")
    private String port;

    private int batchSize;

    @Scheduled(fixedDelayString = "${recovery.poll-delay}")
    public void recoverStuckJobs() {
        log.info("[Recovery:{}] Looking for stuck jobs...", port);

        List<Job> stuckJobs = jobRepository.findStuckJobs(JobStatus.RUNNING, LocalDateTime.now().minusMinutes(5), PageRequest.of(0, batchSize));

        for (Job job: stuckJobs) {
            log.warn("[Recovery:{}] Recovering stuck job {}", port, job.getId());
            job.setAttemptCount(job.getAttemptCount() + 1);

            if (job.getAttemptCount() >= job.getMaxAttempts()) {
                jobService.markFailed(job.getId());
            }
            else {
                jobService.markPending(job.getId());
            }

            jobRepository.save(job);
        }
    }
}
