package com.example.studentbatch.config;

import com.example.studentbatch.listener.CustomSkipListener;
import com.example.studentbatch.listener.JobCompletionNotificationListener;
import com.example.studentbatch.model.StudentResult;
import com.example.studentbatch.processor.StudentResultItemProcessor;
import com.example.studentbatch.repository.StudentResultRepository;
import com.example.studentbatch.service.BatchJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@Configuration
@EnableBatchProcessing
public class BatchConfig {
    private static final Logger log = LoggerFactory.getLogger(BatchJobService.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final StudentResultRepository studentResultRepository;
    private final StudentResultItemProcessor processor;
    private final JobCompletionNotificationListener listener;

    @Value("${batch.chunk.size:100}")
    private int chunkSize;

    public BatchConfig(JobRepository jobRepository,
                       PlatformTransactionManager transactionManager,
                       StudentResultRepository studentResultRepository,
                       StudentResultItemProcessor processor,
                       JobCompletionNotificationListener listener) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.studentResultRepository = studentResultRepository;
        this.processor = processor;
        this.listener = listener;
    }

    @Bean
    @ConditionalOnMissingBean
    public JobLauncher jobLauncher() throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }


    @Bean
    @StepScope
    public FlatFileItemReader<StudentResult> reader(@Value("#{jobParameters['filePath']}") String filePath) {
        log.info("Reading file from path: {}", filePath);

        // Check if file exists and log its size
        File file = new File(filePath);
        if (file.exists()) {
            log.info("File exists. Size: {} bytes", file.length());

            // Try to read first few lines to debug
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                log.info("Total lines in file: {}", lines.size());

                for (int i = 0; i < Math.min(5, lines.size()); i++) {
                    log.info("Line {}: '{}'", i, lines.get(i));
                }

                // Check for empty lines
                long emptyLines = lines.stream().filter(line -> line.trim().isEmpty()).count();
                log.info("Empty lines in file: {}", emptyLines);

            } catch (Exception e) {
                log.error("Error reading file for debugging", e);
            }
        } else {
            log.error("File does not exist: {}", filePath);
        }

        return new FlatFileItemReaderBuilder<StudentResult>()
            .name("studentResultItemReader")
            .resource(new FileSystemResource(filePath))
            .delimited()
            .delimiter(",")
            .names(new String[]{"studentId", "courseName", "score"})
            .linesToSkip(1)
            .strict(true)
            .fieldSetMapper(new BeanWrapperFieldSetMapper<StudentResult>() {{
                setTargetType(StudentResult.class);
                setStrict(false);
            }})
            .build();
    }

    @Bean
    public RepositoryItemWriter<StudentResult> writer() {
        RepositoryItemWriter<StudentResult> writer = new RepositoryItemWriter<>();
        writer.setRepository(studentResultRepository);
        writer.setMethodName("save");
        return writer;
    }

    @Bean
    public Step importStudentResultsStep(FlatFileItemReader<StudentResult> reader, RepositoryItemWriter<StudentResult> writer, CustomSkipListener skipListener) {
        return new StepBuilder("importStudentResultsStep", jobRepository)
            .<StudentResult, StudentResult>chunk(chunkSize, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skipLimit(1000)
            .skip(Exception.class)
            .listener(skipListener)
            .listener(skipListener)
            .build();
    }


    @Bean
    public Job importStudentResultsJob(Step importStudentResultsStep) {
        return new JobBuilder("importStudentResultsJob", jobRepository)
            .listener(listener)
            .flow(importStudentResultsStep)
            .end()
            .build();
    }


}