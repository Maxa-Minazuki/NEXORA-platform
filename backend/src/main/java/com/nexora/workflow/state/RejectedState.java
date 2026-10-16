package com.nexora.workflow.state;

import com.nexora.workflow.entity.DocumentStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Terminal. A rejected request must be filed again as a new document. */
@Component
public class RejectedState implements DocumentState {
    @Override public DocumentStatus status() { return DocumentStatus.REJECTED; }
    @Override public Set<String> allowedActions() { return Set.of(); }
}
