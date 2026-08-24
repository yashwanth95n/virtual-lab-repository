package com.ledger.repository;

import com.ledger.model.LabActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LabActivityRepository extends JpaRepository<LabActivity, Long> {
    List<LabActivity> findByLabVmIdOrderByCreatedAtDesc(Long labVmId);
    List<LabActivity> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<LabActivity> findAllByOrderByCreatedAtDesc();
}
