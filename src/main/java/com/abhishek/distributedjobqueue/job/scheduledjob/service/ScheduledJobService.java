package com.abhishek.distributedjobqueue.job.scheduledjob.service;

import com.abhishek.distributedjobqueue.job.scheduledjob.entity.ScheduledJob;
import com.abhishek.distributedjobqueue.job.scheduledjob.enums.ScheduledJobStatus;
import com.abhishek.distributedjobqueue.job.scheduledjob.repository.ScheduledJobRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ConfigurationProperties(prefix = "worker")
@Service
@Setter
@Getter
@RequiredArgsConstructor
public class ScheduledJobService {

    private final ScheduledJobRepository scheduledJobRepository;

    private int batchSize;

    public ScheduledJob createScheduledJob(ScheduledJob scheduledJob) {

        scheduledJob.setStatus(ScheduledJobStatus.PENDING);

        if (scheduledJob.getAttemptCount() == null) {
            scheduledJob.setAttemptCount(0);
        }

        if (scheduledJob.getMaxAttempts() == null) {
            scheduledJob.setMaxAttempts(3);
        }

        return scheduledJobRepository.save(scheduledJob);
    }

    public Optional<ScheduledJob> getScheduledJobById(UUID id) {
        return scheduledJobRepository.findById(id);
    }

    public List<ScheduledJob> findByStatus(ScheduledJobStatus status) {
        return scheduledJobRepository.findByStatus(status);
    }

    /**
     * Finds due PENDING scheduled jobs, locks them,
     * and changes their status to RUNNING.
     *
     * This is the claim operation used by the scheduler.
     */
    @Transactional
    public List<ScheduledJob> claimDueJobs() {

        List<ScheduledJob> jobs =
                scheduledJobRepository.findDueJobsForUpdate(
                        ScheduledJobStatus.PENDING,
                        LocalDateTime.now(),
                        PageRequest.of(0, batchSize)
                );

        for (ScheduledJob job : jobs) {
            job.setStatus(ScheduledJobStatus.RUNNING);
        }

        return scheduledJobRepository.saveAll(jobs);
    }

    /**
     * Marks a RUNNING scheduled job as successfully completed.
     */
    @Transactional
    public void markCompleted(UUID id) {
        updateStatus(id, ScheduledJobStatus.COMPLETED);
    }

    /**
     * Increments the attempt count.
     *
     * If attempts remain, the job goes back to PENDING
     * so that it can be picked up again.
     *
     * If attempts are exhausted, the job becomes STUCK.
     */
    @Transactional
    public void retryOrStuck(UUID id) {

        ScheduledJob job = scheduledJobRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Scheduled job not found!"));

        if (job.getStatus() != ScheduledJobStatus.RUNNING) {
            throw new RuntimeException(
                    "Only RUNNING scheduled jobs can be retried."
            );
        }

        job.setAttemptCount(job.getAttemptCount() + 1);

        if (job.getAttemptCount() >= job.getMaxAttempts()) {
            job.setStatus(ScheduledJobStatus.STUCK);
        } else {
            job.setStatus(ScheduledJobStatus.PENDING);
        }

        scheduledJobRepository.save(job);
    }

    /**
     * Marks a scheduled job as permanently failed.
     *
     * This should be used for failures that should not be retried.
     */
    @Transactional
    public void markFailed(UUID id) {
        updateStatus(id, ScheduledJobStatus.FAILED);
    }

    /**
     * Updates the status after validating the state transition.
     */
    private void updateStatus(
            UUID id,
            ScheduledJobStatus newStatus
    ) {

        ScheduledJob job = scheduledJobRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Scheduled job not found!"));

        if (job.getStatus() == newStatus) {
            throw new RuntimeException(
                    "Job already in this status."
            );
        }

        if (!isValidTransition(job.getStatus(), newStatus)) {
            throw new RuntimeException(
                    "The transition is not valid!"
            );
        }

        job.setStatus(newStatus);

        scheduledJobRepository.save(job);
    }

    /**
     * Valid scheduled-job state transitions.
     *
     * PENDING  -> RUNNING
     * RUNNING  -> COMPLETED
     * RUNNING  -> PENDING   (retry)
     * RUNNING  -> STUCK     (attempts exhausted)
     * RUNNING  -> FAILED    (non-retryable failure)
     */
    private boolean isValidTransition(
            ScheduledJobStatus oldStatus,
            ScheduledJobStatus newStatus
    ) {

        if (oldStatus == ScheduledJobStatus.PENDING
                && newStatus == ScheduledJobStatus.RUNNING) {
            return true;
        }

        if (oldStatus == ScheduledJobStatus.RUNNING
                && newStatus == ScheduledJobStatus.COMPLETED) {
            return true;
        }

        if (oldStatus == ScheduledJobStatus.RUNNING
                && newStatus == ScheduledJobStatus.PENDING) {
            return true;
        }

        if (oldStatus == ScheduledJobStatus.RUNNING
                && newStatus == ScheduledJobStatus.STUCK) {
            return true;
        }

        return oldStatus == ScheduledJobStatus.RUNNING
                && newStatus == ScheduledJobStatus.FAILED;
    }
}
