package com.nexora.workflow.state;

import com.nexora.shared.exception.InvalidStateTransitionException;
import com.nexora.workflow.entity.Document;
import com.nexora.workflow.entity.DocumentStatus;

import java.time.Instant;
import java.util.Set;

/**
 * <h2>Design pattern: State - document lifecycle</h2>
 *
 * <pre>
 * DRAFT --submit--> PENDING_APPROVAL --approve(step 1 of n)--> IN_REVIEW --approve(last)--> APPROVED
 *   ^                    |  \--approve(last step)----------------------------------------> APPROVED
 *   |                    |--reject--> REJECTED                IN_REVIEW --reject--> REJECTED
 *   \----return for revision---------/------------------------------/
 * DRAFT --submit with empty route (system document)--> APPROVED
 * </pre>
 * APPROVED and REJECTED are terminal: there is no user action that turns them back into a draft.
 */
public interface DocumentState {

    DocumentStatus status();

    Set<String> allowedActions();

    default void edit(Document document) {
        throw new InvalidStateTransitionException("document", status().name(), "edit");
    }

    /** @param routeHasSteps false when the Chain of Responsibility decided no approval is needed */
    default void submit(Document document, boolean routeHasSteps, Instant now) {
        throw new InvalidStateTransitionException("document", status().name(), "submit");
    }

    /** @param lastStep true when the step just approved was the final one of the route */
    default void approve(Document document, boolean lastStep, Instant now) {
        throw new InvalidStateTransitionException("document", status().name(), "approve");
    }

    default void reject(Document document, String reason, Instant now) {
        throw new InvalidStateTransitionException("document", status().name(), "reject");
    }

    default void returnForRevision(Document document, Instant now) {
        throw new InvalidStateTransitionException("document", status().name(), "return for revision");
    }
}
