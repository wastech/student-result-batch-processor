package com.example.studentbatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentOverallResultDto {
    private String studentId;
    private List<StudentResultDetail> courseResults;
    private Double overallAverageScore;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentResultDetail {
        private String courseName;
        private Integer score;
        private String grade;
    }
}