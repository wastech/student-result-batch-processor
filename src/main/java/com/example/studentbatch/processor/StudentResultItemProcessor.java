package com.example.studentbatch.processor;

import com.example.studentbatch.model.StudentResult;
import com.example.studentbatch.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class StudentResultItemProcessor implements ItemProcessor<StudentResult, StudentResult> {

    private static final Logger log = LoggerFactory.getLogger(StudentResultItemProcessor.class);

    @Override
    public StudentResult process(StudentResult studentResult) throws Exception {
        log.info("Processing student result: {}", studentResult);

        log.info("Student ID: '{}', Course: '{}', Score: {}, Grade: '{}'",
            studentResult.getStudentId(),
            studentResult.getCourseName(),
            studentResult.getScore(),
            studentResult.getGrade());

        boolean isValid = ValidationUtils.isValidStudentResult(studentResult);
        log.info("Validation result: {}", isValid);

        if (isValid) {
            // Grade calculation logic
            if (studentResult.getScore() >= 90) {
                studentResult.setGrade("A");
            } else if (studentResult.getScore() >= 80) {
                studentResult.setGrade("B");
            } else if (studentResult.getScore() >= 70) {
                studentResult.setGrade("C");
            } else if (studentResult.getScore() >= 60) {
                studentResult.setGrade("D");
            } else {
                studentResult.setGrade("F");
            }

            log.info("Successfully processed and returning: {}", studentResult);
            return studentResult;
        } else {
            log.warn("REJECTING record due to validation failure: {}", studentResult);
            return null;
        }
    }
}