package com.ledger.repository;
import com.ledger.model.MaterialProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface MaterialProgressRepository extends JpaRepository<MaterialProgress, Long> {
    List<MaterialProgress> findByStudentIdAndCourseId(Long studentId, Long courseId);
    Optional<MaterialProgress> findByStudentIdAndMaterialId(Long studentId, Long materialId);
    long countByStudentIdAndCourseIdAndCompletedTrue(Long studentId, Long courseId);
}
