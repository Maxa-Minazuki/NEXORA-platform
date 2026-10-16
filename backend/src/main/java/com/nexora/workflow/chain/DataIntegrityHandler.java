package com.nexora.workflow.chain;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

/** 2. Values are plausible and consistent with the document type. */
@Component
@Order(20)
public class DataIntegrityHandler extends DocumentCheckHandler {

    static final BigDecimal MAX_AMOUNT = new BigDecimal("100000000.00");

    @Override public String name() { return "DataIntegrityHandler"; }

    @Override
    protected void check(DocumentReview r) {
        if (r.amount() != null && (r.amount().signum() < 0 || r.amount().compareTo(MAX_AMOUNT) > 0)) {
            fail(r, "Сома 0 мен " + MAX_AMOUNT.toPlainString() + " аралығында болуы керек");
            return;
        }
        if (r.amount() != null && r.amount().scale() > 2) {
            fail(r, "Сомада ең көбі 2 ондық белгі болуы мүмкін");
            return;
        }
        if (r.content().trim().length() < 10) {
            fail(r, "Мазмұны тым қысқа (кемінде 10 таңба)");
            return;
        }
        String expected = r.type().relatedEntityType();
        if (r.relatedEntityType() != null && !Objects.equals(expected, r.relatedEntityType())) {
            fail(r, "«" + r.type().label() + "» құжаты " + r.relatedEntityType() + " нысанына сілтеме жасай алмайды");
            return;
        }
        pass(r, "Деректер дұрыс");
    }
}
