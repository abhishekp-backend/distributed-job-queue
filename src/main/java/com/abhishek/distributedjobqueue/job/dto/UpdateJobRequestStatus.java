package com.abhishek.distributedjobqueue.job.dto;

import com.abhishek.distributedjobqueue.job.enums.JobStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateJobRequestStatus {
    private JobStatus status;
}
