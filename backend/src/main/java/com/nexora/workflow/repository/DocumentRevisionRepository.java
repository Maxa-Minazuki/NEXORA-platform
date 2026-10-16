package com.nexora.workflow.repository;

import com.nexora.workflow.entity.DocumentRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRevisionRepository extends JpaRepository<DocumentRevision, Long> {
    List<DocumentRevision> findByDocumentIdOrderByRevisionDesc(Long documentId);
}
