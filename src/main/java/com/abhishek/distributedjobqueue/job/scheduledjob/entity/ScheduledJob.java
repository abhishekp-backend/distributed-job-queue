package com.abhishek.distributedjobqueue.job.scheduledjob.entity;

import com.abhishek.distributedjobqueue.job.scheduledjob.enums.ScheduleType;
import com.abhishek.distributedjobqueue.job.scheduledjob.enums.ScheduledJobStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "scheduled_jobs",
        indexes = {
                @Index(
                        name = "idx_scheduled_jobs_due",
                        columnList = "status, nextRunAt"
                )
        }
)
@Getter
@Setter
public class ScheduledJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduledJobStatus status;

    @Min(1)
    @Max(10)
    @Column(nullable = false)
    private Integer priority = 1;

    @Column(nullable = false)
    private Integer attemptCount = 0;

    @Column(nullable = false)
    private Integer maxAttempts = 3;

    @Version
    private Long version;

    private Integer maxExecutions = 1;
    private Integer executionCount = 0;

    /**
     * First execution time for one-time jobs.
     */
    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    /**
     * Next execution time.
     * For one-time jobs this is initially equal to scheduledAt.
     */
    @Column(nullable = false)
    private LocalDateTime nextRunAt;

    /**
     * Updated after every successful execution.
     */
    private LocalDateTime lastRunAt;

    /**
     * Whether the schedule is still active.
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * ONE_TIME, FIXED_DELAY, FIXED_RATE, CRON
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType scheduleType = ScheduleType.ONE_TIME;

    /**
     * Used for recurring schedules.
     */
    private Long repeatIntervalSeconds;

    /**
     * Optional cron expression.
     */
    private String cronExpression;

    /**
     * Timezone for cron execution.
     */
    private String timezone;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}