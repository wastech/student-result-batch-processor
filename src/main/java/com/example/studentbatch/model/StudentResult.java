package com.example.studentbatch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id")
    private String studentId;

    @Column(name = "course_name")
    private String courseName;

    private Integer score;
    private String grade;

    public StudentResult(String studentId, String courseName, Integer score, String grade) {
        this.studentId = studentId;
        this.courseName = courseName;
        this.score = score;
        this.grade = grade;
    }
}