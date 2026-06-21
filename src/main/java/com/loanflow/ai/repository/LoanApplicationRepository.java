package com.loanflow.ai.repository;

import com.loanflow.ai.model.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByLoanNumber(String loanNumber);
}
