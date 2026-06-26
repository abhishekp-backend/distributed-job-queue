package com.abhishek.distributedjobqueue.job.repository;

import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.enums.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByStatus(JobStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Job j WHERE j.status = :status ORDER BY j.createdAt ASC")
    List<Job> findPendingJobsForUpdate(JobStatus status);

    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.updatedAt < :time")
    List<Job> findStuckJobs(JobStatus status, LocalDateTime time);
}
