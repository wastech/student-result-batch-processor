package com.example.studentbatch.util;

import com.example.studentbatch.model.StudentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationUtils {

    private static final Logger log = LoggerFactory.getLogger(ValidationUtils.class);

    public static boolean isValidStudentResult(StudentResult studentResult) {
        if (studentResult == null) {
            log.warn("StudentResult is null");
            return false;
        }

        // Check student ID
        if (studentResult.getStudentId() == null || studentResult.getStudentId().trim().isEmpty()) {
            log.warn("Invalid studentId: '{}'", studentResult.getStudentId());
            return false;
        }

        // Check course name
        if (studentResult.getCourseName() == null || studentResult.getCourseName().trim().isEmpty()) {
            log.warn("Invalid courseName: '{}'", studentResult.getCourseName());
            return false;
        }

        // Check score
        if (studentResult.getScore() == null || studentResult.getScore() < 0 || studentResult.getScore() > 100) {
            log.warn("Invalid score: {}", studentResult.getScore());
            return false;
        }

        log.info("StudentResult passed validation: {}", studentResult);
        return true;
    }
}