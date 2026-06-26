package com.abhishek.distributedjobqueue.worker;

import com.abhishek.distributedjobqueue.execution.JobExecutor;
import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.enums.JobStatus;
import com.abhishek.distributedjobqueue.job.repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkerService {
    private final JobRepository jobRepository;
    private final JobExecutor jobExecutor;

    @Value("${server.port}")
    private String port;

    @Transactional
    public boolean processNextJob() {
        List<Job> jobs = jobRepository.findPendingJobsForUpdate(JobStatus.PENDING);

        if (jobs == null || jobs.isEmpty()) return false;

        Job job = jobs.getFirst();

        if (job == null) return false;

        job.setStatus(JobStatus.RUNNING);
        jobRepository.save(job);
        log.info("[Worker:{}] Found job {}", port, job.getId());

        try {
            jobExecutor.execute(job);
            job.setStatus(JobStatus.COMPLETED);
        }
        catch (Exception e) {
            job.setAttemptCount(job.getAttemptCount() + 1);

            if (job.getAttemptCount() >= job.getMaxAttempts()) {
                job.setStatus(JobStatus.FAILED);
            }
            else {
                job.setStatus(JobStatus.PENDING);
            }
        }

        jobRepository.save(job);

        return true;
    }
}
