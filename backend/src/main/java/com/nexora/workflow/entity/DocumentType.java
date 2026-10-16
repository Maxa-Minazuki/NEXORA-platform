package com.nexora.workflow.entity;

import com.nexora.shared.security.Role;

import java.util.EnumSet;
import java.util.Set;

/**
 * Business rules per document type. The approval route is not hard-coded here: the Chain of
 * Responsibility reads these flags together with the amount thresholds from PlatformSettings.
 */
public enum DocumentType {

    PURCHASE_REQUEST("Сатып алуға өтінім", EnumSet.of(Role.ADMIN, Role.MANAGER, Role.EMPLOYEE),
            true, null, true, false),
    INVENTORY_REPLENISHMENT("Қорды толықтыру", EnumSet.of(Role.ADMIN, Role.MANAGER, Role.EMPLOYEE),
            true, "PRODUCT", true, false),
    MEDICAL_APPOINTMENT_REPORT("Медициналық қабылдау есебі", EnumSet.of(Role.ADMIN, Role.DOCTOR),
            false, "APPOINTMENT", true, false),
    FLIGHT_BOOKING_CONFIRMATION("Брондауды растау құжаты", EnumSet.noneOf(Role.class),
            false, "FLIGHT_BOOKING", false, true),
    GENERAL_ADMINISTRATIVE("Жалпы әкімшілік құжат",
            EnumSet.of(Role.ADMIN, Role.MANAGER, Role.EMPLOYEE, Role.DOCTOR), false, null, false, false);

    private final String label;
    private final Set<Role> authorRoles;
    private final boolean amountRequired;
    private final String relatedEntityType;
    private final boolean managerReviewAlways;
    private final boolean systemIssued;

    DocumentType(String label, Set<Role> authorRoles, boolean amountRequired, String relatedEntityType,
                 boolean managerReviewAlways, boolean systemIssued) {
        this.label = label;
        this.authorRoles = authorRoles;
        this.amountRequired = amountRequired;
        this.relatedEntityType = relatedEntityType;
        this.managerReviewAlways = managerReviewAlways;
        this.systemIssued = systemIssued;
    }

    public String label() { return label; }
    public Set<Role> authorRoles() { return authorRoles; }
    public boolean amountRequired() { return amountRequired; }
    public String relatedEntityType() { return relatedEntityType; }
    public boolean managerReviewAlways() { return managerReviewAlways; }
    public boolean systemIssued() { return systemIssued; }
}
