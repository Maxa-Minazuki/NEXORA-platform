package com.nexora.workflow.state;

import com.nexora.workflow.entity.DocumentStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Terminal. Editing, re-submitting or "un-approving" are rejected by the interface defaults. */
@Component
public class ApprovedState implements DocumentState {
    @Override public DocumentStatus status() { return DocumentStatus.APPROVED; }
    @Override public Set<String> allowedActions() { return Set.of(); }
}
