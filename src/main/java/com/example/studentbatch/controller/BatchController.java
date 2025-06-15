package com.example.studentbatch.controller;

import com.example.studentbatch.dto.StudentOverallResultDto;
import com.example.studentbatch.exception.JobNotFoundException;
import com.example.studentbatch.service.BatchJobService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final BatchJobService batchJobService;

    public BatchController(BatchJobService batchJobService) {
        this.batchJobService = batchJobService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFileAndStartJob(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }
        try {
            Long jobExecutionId = batchJobService.startImportJob(file);
            return ResponseEntity.ok(Map.of("message", "Batch job started successfully!", "jobExecutionId", jobExecutionId));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file: " + e.getMessage());
        } catch (JobParametersInvalidException | org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error starting job: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/status/{jobExecutionId}")
    public ResponseEntity<?> getJobStatus(@PathVariable Long jobExecutionId) {
        try {
            JobExecution jobExecution = batchJobService.getJobStatus(jobExecutionId);
            return ResponseEntity.ok(Map.of(
                "jobExecutionId", jobExecution.getId(),
                "jobName", jobExecution.getJobInstance().getJobName(),
                "status", jobExecution.getStatus().name(),
                "startTime", jobExecution.getStartTime(),
                "endTime", jobExecution.getEndTime(),
                "exitStatus", jobExecution.getExitStatus().getExitCode()
            ));
        } catch (JobNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/history/{jobName}")
    public ResponseEntity<?> getJobHistory(@PathVariable String jobName) {
        List<JobExecution> history = batchJobService.getJobHistory(jobName);
        if (history.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No history found for job: " + jobName));
        }
        return ResponseEntity.ok(history.stream().map(jobExecution -> Map.of(
            "jobExecutionId", jobExecution.getId(),
            "status", jobExecution.getStatus().name(),
            "startTime", jobExecution.getStartTime(),
            "endTime", jobExecution.getEndTime(),
            "exitStatus", jobExecution.getExitStatus().getExitCode()
        )).toList());
    }

    @PostMapping("/stop/{jobExecutionId}")
    public ResponseEntity<?> stopJob(@PathVariable Long jobExecutionId) {
        try {
            String message = batchJobService.stopJob(jobExecutionId);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (JobNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (NoSuchJobExecutionException | JobExecutionNotRunningException e) {
            return ResponseEntity.badRequest().body("Error stopping job: " + e.getMessage());
        } catch (NoSuchJobException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: Job definition not found: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<?> clearCache() {
        batchJobService.clearRedisCache();
        return ResponseEntity.ok(Map.of("message", "Redis cache cleared successfully!"));
    }


    @GetMapping("/student/{studentId}/results")
    public ResponseEntity<?> getStudentResults(@PathVariable String studentId) {
        StudentOverallResultDto studentResults = batchJobService.getStudentResults(studentId);
        if (studentResults == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No results found for student ID: " + studentId));
        }
        return ResponseEntity.ok(studentResults);
    }
}