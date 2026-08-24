package com.ledger.repository;

import com.ledger.model.LabVm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LabVmRepository extends JpaRepository<LabVm, Long> {
    List<LabVm> findByAssignedStudentId(Long studentId);
    List<LabVm> findByStatus(String status);
}
