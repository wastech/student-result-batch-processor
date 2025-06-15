package com.example.studentbatch.listener;

import com.example.studentbatch.repository.StudentResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

    private final StudentResultRepository repository;

    public JobCompletionNotificationListener(StudentResultRepository repository) {
        this.repository = repository;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("=== JOB EXECUTION SUMMARY ===");
        log.info("Job ID: {}", jobExecution.getJobId());
        log.info("Job Status: {}", jobExecution.getStatus());
        log.info("Start Time: {}", jobExecution.getStartTime());
        log.info("End Time: {}", jobExecution.getEndTime());

        // Step execution details
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            log.info("--- Step: {} ---", stepExecution.getStepName());
            log.info("Read Count: {}", stepExecution.getReadCount());
            log.info("Write Count: {}", stepExecution.getWriteCount());
            log.info("Skip Count: {}", stepExecution.getSkipCount());
            log.info("Filter Count: {}", stepExecution.getFilterCount());
            log.info("Commit Count: {}", stepExecution.getCommitCount());
            log.info("Rollback Count: {}", stepExecution.getRollbackCount());

            // Custom counts from processor
            Integer processedCount = stepExecution.getExecutionContext().getInt("processedCount", 0);
            Integer rejectedCount = stepExecution.getExecutionContext().getInt("rejectedCount", 0);
            log.info("Processed Count: {}", processedCount);
            log.info("Rejected Count: {}", rejectedCount);
        }

        // Database verification
        try {
            long totalRecords = repository.count();
            log.info("📊 Total records in database: {}", totalRecords);

            if (totalRecords == 0) {
                log.error("🚨 NO RECORDS FOUND IN DATABASE! This indicates a problem.");
            }
        } catch (Exception e) {
            log.error("Error checking database count", e);
        }

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("✅ JOB COMPLETED SUCCESSFULLY!");
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("❌ JOB FAILED! Exceptions: {}", jobExecution.getAllFailureExceptions());
        } else {
            log.warn("⚠️ JOB COMPLETED WITH STATUS: {}", jobExecution.getStatus());
        }

        log.info("=== END JOB SUMMARY ===");
    }
}