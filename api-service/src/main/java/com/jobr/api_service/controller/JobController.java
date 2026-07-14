package com.jobr.api_service.controller;

import com.jobr.api_service.dto.CreateJobRequest;
import com.jobr.api_service.entity.Job;
import com.jobr.api_service.repository.JobRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @PostMapping
    public ResponseEntity<Job> createJob(@Valid @RequestBody CreateJobRequest request) throws Exception {

        if (request.idempotencyKey() != null) {
            var existing = jobRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                return ResponseEntity.ok(existing.get());
            }
        }

        Job job = new Job();
        job.setJobType(request.jobType());
        job.setPayload(objectMapper.writeValueAsString(request.payload()));
        job.setPriority(request.priority() != null ? request.priority() : 5);
        job.setCronExpression(request.cronExpression());
        job.setIsRecurring(Boolean.TRUE.equals(request.isRecurring()));
        job.setIdempotencyKey(request.idempotencyKey());
        job.setNextRunTime(Instant.now());

        Job saved = jobRepository.save(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable Long id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}