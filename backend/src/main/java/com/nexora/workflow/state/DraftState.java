package com.nexora.workflow.state;

import com.nexora.workflow.entity.Document;
import com.nexora.workflow.entity.DocumentStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
public class DraftState implements DocumentState {

    @Override public DocumentStatus status() { return DocumentStatus.DRAFT; }

    @Override public Set<String> allowedActions() { return Set.of("edit", "submit"); }

    @Override
    public void edit(Document document) {
        // editing is allowed; the facade applies the field changes and records a revision
    }

    @Override
    public void submit(Document document, boolean routeHasSteps, Instant now) {
        if (routeHasSteps) {
            document.moveTo(DocumentStatus.PENDING_APPROVAL, now);
        } else {
            document.autoApprove(now);
        }
    }
}
