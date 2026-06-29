package com.abhishek.distributedjobqueue.job.controller;

import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.enums.JobStatus;
import com.abhishek.distributedjobqueue.job.service.JobService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@ConfigurationProperties(prefix = "job")
@Setter
@Getter
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;

    private int maxAttempts;

    @PostMapping
    public Job createJob(@RequestBody Job job) {
        return jobService.createJob(job);
    }

    @GetMapping("/{id}")
    public Optional<Job> findJobById(@PathVariable UUID id) {
        return jobService.getJobById(id);
    }

    @GetMapping("/")
    public List<Job> findByStatus(@RequestParam JobStatus status) {
        return jobService.findByStatus(status);
    }

    @PostMapping("/bulk")
    public ResponseEntity<String> createBulkJobs(@RequestParam int count) {

        for (int i = 1; i <= count; i++) {
            Job job = new Job();
            job.setType("EMAIL");
            job.setPayload("Job-" + i);
            job.setMaxAttempts(maxAttempts);
            jobService.createJob(job);
        }

        return ResponseEntity.ok(count + " jobs created.");
    }
}
