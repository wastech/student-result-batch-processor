package com.example.studentbatch.repository;

import com.example.studentbatch.model.StudentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentResultRepository extends JpaRepository<StudentResult, Long> {
    List<StudentResult> findByStudentId(String studentId);
}