package com.abhishek.distributedjobqueue.job.controller;

import com.abhishek.distributedjobqueue.job.dto.UpdateJobRequestStatus;
import com.abhishek.distributedjobqueue.job.entity.Job;
import com.abhishek.distributedjobqueue.job.enums.JobStatus;
import com.abhishek.distributedjobqueue.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;

    @PostMapping
    public Job createJob(@RequestBody Job job) {
        return jobService.createJob(job);
    }

    @GetMapping("/{id}")
    public Optional<Job> findJobById(@PathVariable UUID id) {
        return jobService.getJobById(id);
    }

    @PatchMapping("/{id}/status")
    public Job updateStatus(@PathVariable UUID id, @RequestBody UpdateJobRequestStatus request) {
        return jobService.updateStatus(id, request.getStatus());
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
            jobService.createJob(job);
        }

        return ResponseEntity.ok(count + " jobs created.");
    }
}
