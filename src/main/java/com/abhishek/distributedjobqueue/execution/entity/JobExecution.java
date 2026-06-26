package com.abhishek.distributedjobqueue.execution.entity;

import com.abhishek.distributedjobqueue.execution.enums.ExecutionStatus;
import com.abhishek.distributedjobqueue.job.entity.Job;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.NumberFormat;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_executions")
public class JobExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @Min(1)
    private Integer maxRetries;

    @Nullable
    private String workerId;

    @Enumerated(EnumType.STRING)
    @NotNull
    private ExecutionStatus status;

    private LocalDateTime startedAt;
    @Nullable
    private LocalDateTime completedAt;

    @Nullable()
    @Size(max = 500)
    private String errorMessage;
}
