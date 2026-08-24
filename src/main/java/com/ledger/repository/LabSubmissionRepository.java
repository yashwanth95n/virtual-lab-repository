package com.ledger.repository;
import com.ledger.model.LabSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface LabSubmissionRepository extends JpaRepository<LabSubmission, Long> {
    List<LabSubmission> findByStudentId(Long studentId);
    List<LabSubmission> findByLabId(Long labId);
}
