package com.abhishek.distributedjobqueue.job.service;

import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.enums.JobStatus;
import com.abhishek.distributedjobqueue.job.repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ConfigurationProperties(prefix = "worker")
@Service
@Setter
@Getter
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    private int batchSize;

    public Job createJob(Job job) {
        job.setStatus(JobStatus.PENDING);
        return jobRepository.save(job);
    }

    public Optional<Job> getJobById(UUID id) {
        return jobRepository.findById(id);
    }

    public List<Job> findByStatus(JobStatus status) {
        return jobRepository.findByStatus(status);
    }

    @Transactional
    public void markPending(UUID id) {
        updateStatus(id, JobStatus.PENDING);
    }

    @Transactional
    public void markRunning(UUID id) {
        updateStatus(id, JobStatus.RUNNING);
    }

    @Transactional
    public void markCompleted(UUID id) {
        updateStatus(id, JobStatus.COMPLETED);
    }

    @Transactional
    public void retryOrFail(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found!"));

        if (job.getStatus() != JobStatus.RUNNING) {
            throw new RuntimeException("Only RUNNING jobs can be retried.");
        }

        job.setAttemptCount(job.getAttemptCount() + 1);

        if (job.getAttemptCount() >= job.getMaxAttempts()) {
            job.setStatus(JobStatus.FAILED);
        } else {
            job.setStatus(JobStatus.PENDING);
        }

        jobRepository.save(job);
    }

    @Transactional
    public void markFailed(UUID id) {
        updateStatus(id, JobStatus.FAILED);
    }

    @Transactional
    public List<Job> claimPendingJobs() {

        List<Job> jobs = jobRepository.findPendingJobsForUpdate(
                JobStatus.PENDING,
                PageRequest.of(0, batchSize));

        for (Job job : jobs) {
            job.setStatus(JobStatus.RUNNING);
        }

        return jobRepository.saveAll(jobs);
    }

    private void updateStatus(UUID id, JobStatus newStatus) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found!"));

        if (job.getStatus() == newStatus) {
            throw new RuntimeException("Job already in this status.");
        }

        if (!isValidTransition(job.getStatus(), newStatus)) {
            throw new RuntimeException("The transition is not valid!");
        }

        job.setStatus(newStatus);
        jobRepository.save(job);
    }

    private boolean isValidTransition(JobStatus oldStatus, JobStatus newStatus) {

        if (oldStatus == JobStatus.PENDING
                && newStatus == JobStatus.RUNNING) {
            return true;
        }

        if (oldStatus == JobStatus.RUNNING
                && newStatus == JobStatus.COMPLETED) {
            return true;
        }

        return oldStatus == JobStatus.RUNNING
                && newStatus == JobStatus.FAILED;
    }
}