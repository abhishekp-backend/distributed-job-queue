package com.abhishek.distributedjobqueue.job.scheduledjob.repository;

import com.abhishek.distributedjobqueue.job.scheduledjob.entity.ScheduledJob;
import com.abhishek.distributedjobqueue.job.scheduledjob.enums.ScheduledJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, UUID> {

    List<ScheduledJob> findByStatus(ScheduledJobStatus status);

    @Query(value = """
        SELECT *
        FROM scheduled_jobs sj
        WHERE sj.status = :status
          AND sj.enabled = true
          AND sj.next_run_at <= :now
        ORDER BY sj.priority DESC,
                 sj.next_run_at ASC
        FOR UPDATE SKIP LOCKED
        """,
            nativeQuery = true)
    List<ScheduledJob> findDueJobsForUpdate(
            @Param("status") ScheduledJobStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

}