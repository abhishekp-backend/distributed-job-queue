package com.abhishek.distributedjobqueue.execution.service;

import com.abhishek.distributedjobqueue.execution.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobExecutionService {
    private final JobExecutionRepository jobExecutionRepository;
}
