package com.nexora.workflow.repository;

import com.nexora.workflow.entity.DocumentAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentActionRepository extends JpaRepository<DocumentAction, Long> {
    List<DocumentAction> findByDocumentIdOrderByCreatedAtAscIdAsc(Long documentId);
}
