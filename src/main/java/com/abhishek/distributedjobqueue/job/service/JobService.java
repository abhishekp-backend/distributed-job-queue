package com.abhishek.distributedjobqueue.job.service;

import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.enums.JobStatus;
import com.abhishek.distributedjobqueue.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;

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

    public Job updateStatus(UUID id, JobStatus newStatus) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Job not found!"));

        if (job.getStatus() == newStatus) {
            throw new RuntimeException("Job already in this status");
        }

        JobStatus oldStatus = job.getStatus();

        if (!isValidTransition(oldStatus, newStatus)) {
            throw new RuntimeException("The transition is not valid!");
        }

        job.setStatus(newStatus);

        return jobRepository.save(job);
    }

    private boolean isValidTransition(
                JobStatus oldStatus,
                JobStatus newStatus) {

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
