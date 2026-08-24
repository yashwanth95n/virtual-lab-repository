package com.ledger.repository;
import com.ledger.model.Lab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface LabRepository extends JpaRepository<Lab, Long> {}
