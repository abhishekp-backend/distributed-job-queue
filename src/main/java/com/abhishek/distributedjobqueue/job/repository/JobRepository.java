package com.abhishek.distributedjobqueue.job.repository;

import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.enums.JobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByStatus(JobStatus status);

    @Query(nativeQuery = true,
            value = """
        SELECT *
        FROM jobs j
        WHERE j.status = :status
        ORDER BY j.priority DESC,
                 j.created_at ASC
        FOR UPDATE SKIP LOCKED
        """)
    List<Job> findPendingJobsForUpdate(
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
        SELECT j
        FROM Job j
        WHERE j.status = :status
          AND j.updatedAt < :time
    """)
    List<Job> findStuckJobs(
            @Param("status") JobStatus status,
            @Param("time") LocalDateTime time,
            Pageable pageable
    );
}