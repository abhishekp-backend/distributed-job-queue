package com.abhishek.distributedjobqueue.execution.repository;

import com.abhishek.distributedjobqueue.execution.entity.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {
}
