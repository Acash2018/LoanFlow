package com.loanflow.ai.repository;

import com.loanflow.ai.model.LoanInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanInsightRepository extends JpaRepository<LoanInsight, Long> {

    List<LoanInsight> findByLoanApplicationId(Long loanApplicationId);
}
