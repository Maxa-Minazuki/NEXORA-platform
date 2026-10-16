package com.nexora.patterns;

import com.nexora.shared.config.PlatformSettings;
import com.nexora.shared.security.Role;
import com.nexora.support.TestFixtures;
import com.nexora.workflow.chain.AdditionalApprovalHandler;
import com.nexora.workflow.chain.AuthorPermissionHandler;
import com.nexora.workflow.chain.DataIntegrityHandler;
import com.nexora.workflow.chain.DocumentCheckHandler;
import com.nexora.workflow.chain.DocumentProcessingChain;
import com.nexora.workflow.chain.DocumentReview;
import com.nexora.workflow.chain.ManagerApprovalHandler;
import com.nexora.workflow.chain.RequiredFieldsHandler;
import com.nexora.workflow.chain.RoutingDecisionHandler;
import com.nexora.workflow.entity.DocumentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Chain of Responsibility: each handler checks one thing and either passes the review on or stops it. */
class ChainOfResponsibilityTest {

    private final PlatformSettings settings = TestFixtures.settings();

    private DocumentProcessingChain chain(DocumentCheckHandler... extra) {
        List<DocumentCheckHandler> handlers = new ArrayList<>(List.of(new RequiredFieldsHandler(), new DataIntegrityHandler(),
                new AuthorPermissionHandler(), new ManagerApprovalHandler(settings), new AdditionalApprovalHandler(settings)));
        handlers.addAll(List.of(extra));
        handlers.add(new RoutingDecisionHandler());
        return new DocumentProcessingChain(handlers);
    }

    private static DocumentReview review(DocumentType type, String amount, Role role) {
        return new DocumentReview(type, "Title", "Long enough content for review", amount == null ? null : new BigDecimal(amount),
                null, null, role, false);
    }

    @Test
    void highValuePurchaseGetsManagerAndFinalApproval() {
        DocumentReview r = chain().review(review(DocumentType.PURCHASE_REQUEST, "2000000.00", Role.EMPLOYEE));
        assertThat(r.stopped()).isFalse();
        assertThat(r.steps()).extracting(DocumentReview.PlannedStep::name).containsExactly("Менеджер тексеруі", "Соңғы бекіту");
        assertThat(r.steps()).extracting(DocumentReview.PlannedStep::requiredRole).containsExactly(Role.MANAGER, Role.ADMIN);
        assertThat(r.trace()).extracting(DocumentReview.TraceEntry::handler).containsExactly("RequiredFieldsHandler",
                "DataIntegrityHandler", "AuthorPermissionHandler", "ManagerApprovalHandler", "AdditionalApprovalHandler",
                "RoutingDecisionHandler");
    }

    @Test
    void missingAmountStopsTheChainAtTheFirstHandler() {
        DocumentReview r = chain().review(review(DocumentType.PURCHASE_REQUEST, null, Role.EMPLOYEE));
        assertThat(r.stopped()).isTrue();
        assertThat(r.failure()).contains("сома міндетті");
        assertThat(r.trace()).hasSize(1);
        assertThat(r.steps()).isEmpty();
    }

    @Test
    void authorWithoutPermissionIsStopped() {
        DocumentReview r = chain().review(review(DocumentType.MEDICAL_APPOINTMENT_REPORT, null, Role.EMPLOYEE));
        // required fields fail first (no related appointment) - give one to reach the permission handler
        DocumentReview withRelation = new DocumentReview(DocumentType.MEDICAL_APPOINTMENT_REPORT, "Report",
                "Summary of the visit for review", null, "APPOINTMENT", 5L, Role.EMPLOYEE, false);
        assertThat(r.stopped()).isTrue();
        DocumentReview result = chain().review(withRelation);
        assertThat(result.stopped()).isTrue();
        assertThat(result.failure()).contains("жасай алмайды");
        assertThat(result.trace().getLast().handler()).isEqualTo("AuthorPermissionHandler");
    }

    @Test
    void smallGeneralDocumentStillGetsFourEyesReview() {
        DocumentReview r = chain().review(review(DocumentType.GENERAL_ADMINISTRATIVE, null, Role.DOCTOR));
        assertThat(r.steps()).extracting(DocumentReview.PlannedStep::reason).containsExactly("«төрт көз» қағидаты");
        assertThat(r.decision()).startsWith("МАРШРУТ");
    }

    @Test
    void systemDocumentIsAutoApproved() {
        DocumentReview r = chain().review(new DocumentReview(DocumentType.FLIGHT_BOOKING_CONFIRMATION, "Confirmation",
                "Booking confirmation content", null, "FLIGHT_BOOKING", 3L, Role.USER, true));
        assertThat(r.stopped()).isFalse();
        assertThat(r.steps()).isEmpty();
        assertThat(r.decision()).startsWith("АВТОБЕКІТУ");
    }

    @Test
    void newHandlerIsAddedWithoutChangingOthers() {
        DocumentCheckHandler noWeekendWork = new DocumentCheckHandler() {
            @Override public String name() { return "ComplianceHandler"; }
            @Override protected void check(DocumentReview r) {
                if (r.title().contains("forbidden")) {
                    fail(r, "Compliance rule violated");
                } else {
                    pass(r, "Compliant");
                }
            }
        };
        DocumentReview r = chain(noWeekendWork).review(new DocumentReview(DocumentType.GENERAL_ADMINISTRATIVE, "forbidden title",
                "Some long enough content", null, null, null, Role.EMPLOYEE, false));
        assertThat(r.stopped()).isTrue();
        assertThat(r.trace().getLast().handler()).isEqualTo("ComplianceHandler");
    }
}
