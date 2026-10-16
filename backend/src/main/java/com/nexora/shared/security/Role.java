package com.nexora.shared.security;

/** Platform roles. Authorities are exposed to Spring Security as {@code ROLE_<name>}. */
public enum Role {
    ADMIN, MANAGER, DOCTOR, EMPLOYEE, USER;

    public String authority() {
        return "ROLE_" + name();
    }
}
