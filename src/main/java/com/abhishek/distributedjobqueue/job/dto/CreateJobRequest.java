package com.abhishek.distributedjobqueue.job.dto;

import com.abhishek.distributedjobqueue.job.enums.JobStatus;

public class CreateJobRequest {
    private String type;
    private String payload;
    private Integer priority;
}

