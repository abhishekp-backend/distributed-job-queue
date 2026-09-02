package com.abhishek.distributedjobqueue.service;

import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.enums.JobStatus;
import com.abhishek.distributedjobqueue.job.repository.JobRepository;
import com.abhishek.distributedjobqueue.job.service.JobService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobService jobService;

    @Test
    void shouldCreateJob() {
        Job job = new Job();

        when(jobRepository.save(job)).thenReturn(job);

        Job result = jobService.createJob(job);

        assertEquals(JobStatus.PENDING, result.getStatus());
        verify(jobRepository).save(job);
    }

    @Test
    void shouldGetJobByIdWhenJobExists() {
        UUID id = UUID.randomUUID();
        Job job = new Job();

        when(jobRepository.findById(id))
                .thenReturn(Optional.of(job));

        Optional<Job> result = jobService.getJobById(id);

        assertTrue(result.isPresent());
        assertEquals(job, result.get());

        verify(jobRepository).findById(id);
    }

    @Test
    void shouldGetJobsByStatus() {
        Job job = new Job();
        job.setStatus(JobStatus.PENDING);

        List<Job> jobs = List.of(job);

        when(jobRepository.findByStatus(JobStatus.PENDING))
                .thenReturn(jobs);

        List<Job> result = jobService.findByStatus(JobStatus.PENDING);

        assertEquals(jobs, result);

        verify(jobRepository).findByStatus(JobStatus.PENDING);
    }

    @Test
    void shouldGetPendingJobAsRunning() {
        UUID id = UUID.randomUUID();

        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.PENDING);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        jobService.markRunning(id);

        assertEquals(JobStatus.RUNNING, job.getStatus());
        verify(jobRepository).save(job);
    }

    @Test
    void shouldGetRunningJobAsCompleted() {
        UUID id = UUID.randomUUID();

        Job job = new Job();
        job.setStatus(JobStatus.RUNNING);
        job.setId(id);

        when(jobRepository.findById(id))
                .thenReturn(Optional.of(job));

        jobService.markCompleted(id);

        assertEquals(JobStatus.COMPLETED, job.getStatus());
        verify(jobRepository).save(job);
    }

    @Test
    void shouldGetRunningJobAsFailed() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.RUNNING);

        when(jobRepository.findById(id))
                .thenReturn(Optional.of(job));

        jobService.markFailed(job.getId());

        assertEquals(JobStatus.FAILED, job.getStatus());
        verify(jobRepository).save(job);
    }

    @Test
    void shouldRejectPendingToCompleted() {
        UUID id = UUID.randomUUID();

        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.PENDING);

        when(jobRepository.findById(id))
                .thenReturn(Optional.of(job));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> jobService.markCompleted(id)
        );

        assertEquals("The transition is not valid!", exception.getMessage());
        verify(jobRepository).findById(id);
        verify(jobRepository, never()).save(job);
    }

    @Test
    void shouldRejectPendingToFailed() {
        UUID id = UUID.randomUUID();

        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.PENDING);

        when(jobRepository.findById(id))
                .thenReturn(Optional.of(job));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> jobService.markFailed(id)
        );

        assertEquals("The transition is not valid!", exception.getMessage());

        verify(jobRepository).findById(id);
        verify(jobRepository, never()).save(job);
    }

    @Test
    void shouldRejectRunningToPending() {
        UUID id = UUID.randomUUID();

        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.RUNNING);

        when(jobRepository.findById(id))
                .thenReturn(Optional.of(job));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> jobService.markPending(id)
        );

        assertEquals("The transition is not valid!", exception.getMessage());

        verify(jobRepository).findById(id);
        verify(jobRepository, never()).save(job);
    }

    @Test
    void shouldRejectSameStatusTransition() {
        UUID id = UUID.randomUUID();

        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.RUNNING);

        when(jobRepository.findById(id))
                .thenReturn(Optional.of(job));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> jobService.markRunning(id)
        );

        assertEquals("Job already in this status.", exception.getMessage());

        verify(jobRepository).findById(id);
        verify(jobRepository, never()).save(job);
    }

    @Test
    void shouldRetryRunningJobWhenAttemptsRemain() {
        UUID id = UUID.randomUUID();

        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.RUNNING);
        job.setAttemptCount(0);
        job.setMaxAttempts(3);

        when(jobRepository.findById(id))
                .thenReturn(Optional.of(job));

        jobService.retryOrFail(id);

        assertEquals(1, job.getAttemptCount());
        assertEquals(JobStatus.PENDING, job.getStatus());

        verify(jobRepository).findById(id);
        verify(jobRepository).save(job);
    }

    @Test
    void shouldRetryRunningJobOnSecondAttempt() {
        UUID id = UUID.randomUUID();

        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.RUNNING);
        job.setAttemptCount(1);
        job.setMaxAttempts(3);

        when(jobRepository.findById(id))
                .thenReturn(Optional.of(job));

        jobService.retryOrFail(id);

        assertEquals(2, job.getAttemptCount());
        assertEquals(JobStatus.PENDING, job.getStatus());

        verify(jobRepository).findById(id);
        verify(jobRepository).save(job);
    }

    @Test
    void shouldFailJobWhenMaxAttemptsReached() {
        UUID id = UUID.randomUUID();

        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.RUNNING);
        job.setAttemptCount(2);
        job.setMaxAttempts(3);

        when(jobRepository.findById(id))
                .thenReturn(Optional.of(job));

        jobService.retryOrFail(id);

        assertEquals(3, job.getAttemptCount());
        assertEquals(JobStatus.FAILED, job.getStatus());

        verify(jobRepository).findById(id);
        verify(jobRepository).save(job);
    }

    @Test
    void shouldRejectRetryForNonRunningJob() {
        UUID id = UUID.randomUUID();

        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.PENDING);
        job.setAttemptCount(0);
        job.setMaxAttempts(3);

        when(jobRepository.findById(id))
                .thenReturn(Optional.of(job));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> jobService.retryOrFail(id)
        );

        assertEquals("Only RUNNING jobs can be retried.", exception.getMessage());

        verify(jobRepository).findById(id);
        verify(jobRepository, never()).save(job);
    }

    @Test
    void shouldThrowWhenJobDoesNotExist() {
        UUID id = UUID.randomUUID();

        when(jobRepository.findById(id))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> jobService.markRunning(id)
        );

        assertEquals("Job not found!", exception.getMessage());

        verify(jobRepository).findById(id);
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void shouldClaimPendingJobs() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Job job1 = new Job();
        job1.setId(id1);
        job1.setStatus(JobStatus.PENDING);

        Job job2 = new Job();
        job2.setId(id2);
        job2.setStatus(JobStatus.PENDING);

        List<Job> jobs = List.of(job1, job2);

        jobService.setBatchSize(2);

        when(jobRepository.findPendingJobsForUpdate(
                JobStatus.PENDING.name(),
                PageRequest.of(0, 2)
        )).thenReturn(jobs);

        when(jobRepository.saveAll(jobs))
                .thenReturn(jobs);

        List<Job> result = jobService.claimPendingJobs();

        assertEquals(2, result.size());
        assertEquals(JobStatus.RUNNING, job1.getStatus());
        assertEquals(JobStatus.RUNNING, job2.getStatus());

        verify(jobRepository).findPendingJobsForUpdate(
                JobStatus.PENDING.name(),
                PageRequest.of(0, 2)
        );

        verify(jobRepository).saveAll(jobs);
    }

    @Test
    void shouldReturnEmptyWhenNoPendingJobs() {
        jobService.setBatchSize(10);

        List<Job> emptyJobs = List.of();

        when(jobRepository.findPendingJobsForUpdate(
                JobStatus.PENDING.name(),
                PageRequest.of(0, 10)
        )).thenReturn(emptyJobs);

        when(jobRepository.saveAll(emptyJobs))
                .thenReturn(emptyJobs);

        List<Job> result = jobService.claimPendingJobs();

        assertTrue(result.isEmpty());

        verify(jobRepository).findPendingJobsForUpdate(
                JobStatus.PENDING.name(),
                PageRequest.of(0, 10)
        );

        verify(jobRepository).saveAll(emptyJobs);
    }
}