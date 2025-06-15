package com.example.studentbatch.service;

import com.example.studentbatch.dto.StudentOverallResultDto;
import com.example.studentbatch.exception.JobNotFoundException;
import com.example.studentbatch.model.StudentResult;
import com.example.studentbatch.repository.StudentResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BatchJobService {

    private static final Logger log = LoggerFactory.getLogger(BatchJobService.class);
    private final StudentResultRepository studentResultRepository;
    private final JobLauncher jobLauncher;
    private final Job importStudentResultsJob;
    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${batch.upload.directory:/tmp/batch-uploads}")
    private String uploadDirectory;

    public BatchJobService(JobLauncher jobLauncher,
                           Job importStudentResultsJob,
                           JobExplorer jobExplorer,
                           JobOperator jobOperator,
                           RedisTemplate<String, Object> redisTemplate,StudentResultRepository studentResultRepository) {
        this.jobLauncher = jobLauncher;
        this.importStudentResultsJob = importStudentResultsJob;
        this.jobExplorer = jobExplorer;
        this.jobOperator = jobOperator;
        this.redisTemplate = redisTemplate;
        this.studentResultRepository = studentResultRepository;
    }

    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDirectory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            } else {
                log.info("Upload directory already exists: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}. Using temporary directory as fallback.", uploadDirectory, e);
            // Fallback to system temp directory
            uploadDirectory = System.getProperty("java.io.tmpdir") + File.separator + "batch-uploads";
            try {
                Files.createDirectories(Paths.get(uploadDirectory));
                log.info("Created fallback upload directory: {}", uploadDirectory);
            } catch (IOException fallbackException) {
                log.error("Failed to create fallback upload directory", fallbackException);
                throw new RuntimeException("Cannot create upload directory", fallbackException);
            }
        }
    }

    public Long startImportJob(MultipartFile file) throws IOException, JobExecutionException {
        // Ensure the upload directory exists (double-check)
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Create a unique file name to avoid conflicts if multiple files with the same name are uploaded
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            originalFilename = "uploaded_file";
        }
        String uniqueFilename = System.currentTimeMillis() + "_" + originalFilename;
        Path filePath = Paths.get(uploadDirectory, uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        log.info("Starting job for file: {}", filePath.toAbsolutePath());

        JobParameters jobParameters = new JobParametersBuilder()
            .addString("filePath", filePath.toAbsolutePath().toString())
            .addLong("time", System.currentTimeMillis())
            .toJobParameters();

        try {
            JobExecution jobExecution = jobLauncher.run(importStudentResultsJob, jobParameters);
            log.info("Job started with ID: {}", jobExecution.getId());
            return jobExecution.getId();
        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("Job for file {} already completed: {}", filePath.getFileName(), e.getMessage());
            throw e;
        } catch (JobExecutionException e) {
            log.error("Error launching job for file {}: {}", filePath.getFileName(), e.getMessage(), e);
            throw e;
        }
    }

    public JobExecution getJobStatus(Long jobExecutionId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
        if (jobExecution == null) {
            throw new JobNotFoundException("Job execution with ID " + jobExecutionId + " not found.");
        }
        return jobExecution;
    }

    public List<JobExecution> getJobHistory(String jobName) {
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, 100);
        return jobInstances.stream()
            .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
            .sorted((je1, je2) -> je2.getCreateTime().compareTo(je1.getCreateTime()))
            .collect(Collectors.toList());
    }

    public String stopJob(Long jobExecutionId) throws JobExecutionException, JobNotFoundException {
        JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
        if (jobExecution == null) {
            throw new JobNotFoundException("Job execution with ID " + jobExecutionId + " not found.");
        }

        if (jobExecution.isRunning()) {
            jobOperator.stop(jobExecutionId);
            return "Job " + jobExecutionId + " stopping.";
        } else {
            return "Job " + jobExecutionId + " is not running or already stopped.";
        }
    }

    public void clearRedisCache() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} keys from Redis cache.", keys.size());
            } else {
                log.info("Redis cache is already empty.");
            }
        } catch (Exception e) {
            log.error("Error clearing Redis cache", e);
            throw new RuntimeException("Failed to clear Redis cache", e);
        }
    }


    public StudentOverallResultDto getStudentResults(String studentId) {
        List<StudentResult> results = studentResultRepository.findByStudentId(studentId);

        if (results.isEmpty()) {
            return null;
        }

        // Map StudentResult entities to StudentResultDetail DTOs
        List<StudentOverallResultDto.StudentResultDetail> courseDetails = results.stream()
            .map(sr -> new StudentOverallResultDto.StudentResultDetail(
                sr.getCourseName(),
                sr.getScore(),
                sr.getGrade()
            ))
            .collect(Collectors.toList());

        // Calculate overall average score
        OptionalDouble averageScore = results.stream()
            .mapToInt(StudentResult::getScore)
            .average();

        return new StudentOverallResultDto(
            studentId,
            courseDetails,
            averageScore.isPresent() ? averageScore.getAsDouble() : 0.0
        );
    }
}