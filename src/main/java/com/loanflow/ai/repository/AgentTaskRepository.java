package com.loanflow.ai.repository;

import com.loanflow.ai.model.AgentTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {

    List<AgentTask> findByLoanApplicationId(Long loanApplicationId);
}
