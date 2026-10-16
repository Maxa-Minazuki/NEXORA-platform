package com.nexora.patterns;

import com.nexora.shared.exception.InvalidStateTransitionException;
import com.nexora.workflow.entity.Document;
import com.nexora.workflow.entity.DocumentStatus;
import com.nexora.workflow.entity.DocumentType;
import com.nexora.workflow.state.ApprovedState;
import com.nexora.workflow.state.DocumentStateRegistry;
import com.nexora.workflow.state.DraftState;
import com.nexora.workflow.state.InReviewState;
import com.nexora.workflow.state.PendingApprovalState;
import com.nexora.workflow.state.RejectedState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** State pattern: the document lifecycle DRAFT -> PENDING_APPROVAL -> IN_REVIEW -> APPROVED / REJECTED. */
class DocumentStatePatternTest {

    private final DocumentStateRegistry states = new DocumentStateRegistry(List.of(new DraftState(),
            new PendingApprovalState(), new InReviewState(), new ApprovedState(), new RejectedState()));
    private final Instant now = Instant.parse("2026-10-16T10:00:00Z");

    private Document draft() {
        return new Document(DocumentType.PURCHASE_REQUEST, "Laptops", "Ten laptops for the team", new BigDecimal("10.00"),
                1L, null, null);
    }

    @Test
    void twoStepRouteGoesThroughInReviewToApproved() {
        Document d = draft();
        states.of(d).submit(d, true, now);
        assertThat(d.getStatus()).isEqualTo(DocumentStatus.PENDING_APPROVAL);
        assertThat(d.getApprovalRound()).isEqualTo(1);
        states.of(d).approve(d, false, now);
        assertThat(d.getStatus()).isEqualTo(DocumentStatus.IN_REVIEW);
        states.of(d).approve(d, true, now);
        assertThat(d.getStatus()).isEqualTo(DocumentStatus.APPROVED);
        assertThat(d.getCompletedAt()).isEqualTo(now);
    }

    @Test
    void emptyRouteIsAutoApproved() {
        Document d = draft();
        states.of(d).submit(d, false, now);
        assertThat(d.getStatus()).isEqualTo(DocumentStatus.APPROVED);
    }

    @Test
    void approvedDocumentCannotGoBackToDraftOrBeEdited() {
        Document d = draft();
        states.of(d).submit(d, false, now);
        assertThatThrownBy(() -> states.of(d).edit(d)).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> states.of(d).returnForRevision(d, now)).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> states.of(d).submit(d, true, now)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void rejectionStoresReasonAndIsTerminal() {
        Document d = draft();
        states.of(d).submit(d, true, now);
        states.of(d).reject(d, "Budget exhausted", now);
        assertThat(d.getStatus()).isEqualTo(DocumentStatus.REJECTED);
        assertThat(d.getRejectionReason()).isEqualTo("Budget exhausted");
        assertThatThrownBy(() -> states.of(d).approve(d, true, now)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void returnForRevisionReopensDraftForTheNextRound() {
        Document d = draft();
        states.of(d).submit(d, true, now);
        states.of(d).returnForRevision(d, now);
        assertThat(d.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        states.of(d).submit(d, true, now);
        assertThat(d.getApprovalRound()).isEqualTo(2);
    }

    @Test
    void draftCannotBeApproved() {
        Document d = draft();
        assertThatThrownBy(() -> states.of(d).approve(d, true, now))
                .isInstanceOf(InvalidStateTransitionException.class).hasMessageContaining("«Қаралама» күйіндегі құжатты бекіту");
    }
}
