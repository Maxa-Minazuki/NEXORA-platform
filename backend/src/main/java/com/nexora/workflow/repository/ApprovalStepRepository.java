package com.nexora.workflow.repository;

import com.nexora.workflow.entity.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
    List<ApprovalStep> findByDocumentIdAndApprovalRoundOrderByStepOrderAsc(Long documentId, int round);
    List<ApprovalStep> findByDocumentIdOrderByApprovalRoundAscStepOrderAsc(Long documentId);
}
