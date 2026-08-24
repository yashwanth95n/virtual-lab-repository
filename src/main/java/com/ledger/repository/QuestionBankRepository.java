package com.ledger.repository;
import com.ledger.model.QuestionBankItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBankItem, Long> {
    List<QuestionBankItem> findByBranch(String branch);
    List<QuestionBankItem> findByTopic(String topic);
}
