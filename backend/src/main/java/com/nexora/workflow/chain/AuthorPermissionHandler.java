package com.nexora.workflow.chain;

import com.nexora.shared.i18n.Kk;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 3. The author's role may file this type of document (system documents are issued by the platform). */
@Component
@Order(30)
public class AuthorPermissionHandler extends DocumentCheckHandler {

    @Override public String name() { return "AuthorPermissionHandler"; }

    @Override
    protected void check(DocumentReview r) {
        if (r.type().systemIssued()) {
            if (!r.systemIssued()) {
                fail(r, "«" + r.type().label() + "» құжатын тек платформа шығарады");
                return;
            }
            pass(r, "Жүйе шығарған құжат");
            return;
        }
        if (!r.type().authorRoles().contains(r.authorRole())) {
            fail(r, Kk.role(r.authorRole().name()) + " рөлі «" + r.type().label() + "» құжатын жасай алмайды");
            return;
        }
        pass(r, Kk.role(r.authorRole().name()) + " рөлі «" + r.type().label() + "» құжатын жасай алады");
    }
}
