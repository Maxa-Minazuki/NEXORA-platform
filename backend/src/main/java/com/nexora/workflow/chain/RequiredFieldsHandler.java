package com.nexora.workflow.chain;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 1. Mandatory fields are present for the document type. */
@Component
@Order(10)
public class RequiredFieldsHandler extends DocumentCheckHandler {

    @Override public String name() { return "RequiredFieldsHandler"; }

    @Override
    protected void check(DocumentReview r) {
        if (r.title() == null || r.title().isBlank()) {
            fail(r, "Тақырып міндетті");
            return;
        }
        if (r.content() == null || r.content().isBlank()) {
            fail(r, "Мазмұны міндетті");
            return;
        }
        if (r.type().amountRequired() && r.amount() == null) {
            fail(r, "«" + r.type().label() + "» үшін сома міндетті");
            return;
        }
        if (r.type().relatedEntityType() != null && r.relatedEntityId() == null) {
            fail(r, "«" + r.type().label() + "» байланысты нысанға сілтеме жасауы керек (" + relatedLabel(r.type().relatedEntityType()) + ")");
            return;
        }
        pass(r, "Барлық міндетті өрістер толтырылған");
    }

    private static String relatedLabel(String type) {
        return switch (type) {
            case "PRODUCT" -> "тауар";
            case "APPOINTMENT" -> "қабылдау";
            case "FLIGHT_BOOKING" -> "брондау";
            default -> type;
        };
    }
}
