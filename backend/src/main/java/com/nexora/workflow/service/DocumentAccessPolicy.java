package com.nexora.workflow.service;

import com.nexora.shared.exception.NotFoundException;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.shared.security.Role;
import com.nexora.workflow.entity.Document;
import org.springframework.stereotype.Component;

/** Managers and admins see every document; everybody else sees only documents they authored. */
@Component
public class DocumentAccessPolicy {

    public boolean seesAll(AuthenticatedUser user) {
        return user.hasRole(Role.ADMIN, Role.MANAGER);
    }

    public void checkCanView(AuthenticatedUser user, Document d) {
        if (!seesAll(user) && !d.getAuthorId().equals(user.id())) {
            throw new NotFoundException("Document", d.getId()); // do not reveal existence
        }
    }

    public void checkIsAuthor(AuthenticatedUser user, Document d) {
        if (!d.getAuthorId().equals(user.id())) {
            throw new com.nexora.shared.exception.ForbiddenOperationException("Мұны тек құжат авторы жасай алады");
        }
    }
}
