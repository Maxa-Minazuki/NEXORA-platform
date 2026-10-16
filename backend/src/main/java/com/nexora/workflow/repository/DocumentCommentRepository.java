package com.nexora.workflow.repository;

import com.nexora.workflow.entity.DocumentComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentCommentRepository extends JpaRepository<DocumentComment, Long> {
    List<DocumentComment> findByDocumentIdOrderByCreatedAtAsc(Long documentId);
}
