package com.nexora.workflow.entity;

/** Persisted name of the current State object of a {@link Document}. */
public enum DocumentStatus {
    DRAFT, PENDING_APPROVAL, IN_REVIEW, APPROVED, REJECTED
}
