package com.nexora.workflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "document_comments")
public class DocumentComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DocumentComment() {
    }

    public DocumentComment(Long documentId, Long authorId, String body, Instant createdAt) {
        this.documentId = documentId;
        this.authorId = authorId;
        this.body = body;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getDocumentId() { return documentId; }
    public Long getAuthorId() { return authorId; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
