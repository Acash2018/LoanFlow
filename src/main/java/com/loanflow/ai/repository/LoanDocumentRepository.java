package com.loanflow.ai.repository;

import com.loanflow.ai.model.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanDocumentRepository extends JpaRepository<LoanDocument, Long> {

    List<LoanDocument> findByLoanApplicationId(Long loanApplicationId);
}
