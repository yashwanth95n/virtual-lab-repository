package com.ledger.repository;
import com.ledger.model.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    List<ExamAttempt> findByExamId(Long examId);
    List<ExamAttempt> findByStudentId(Long studentId);
    long countByExamIdAndStudentId(Long examId, Long studentId);
}
