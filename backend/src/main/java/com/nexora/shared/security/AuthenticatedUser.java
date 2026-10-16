package com.nexora.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/** Security principal stored in the HTTP session. Holds identity only, never mutable business state. */
public final class AuthenticatedUser implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String fullName;
    private final Role role;
    private final String passwordHash;
    private final boolean active;

    public AuthenticatedUser(Long id, String username, String fullName, Role role, String passwordHash, boolean active) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.passwordHash = passwordHash;
        this.active = active;
    }

    public Long id() { return id; }
    public String fullName() { return fullName; }
    public Role role() { return role; }

    public boolean hasRole(Role... roles) {
        for (Role r : roles) {
            if (r == role) {
                return true;
            }
        }
        return false;
    }

    /** Copy without the password hash, used once authentication has succeeded. */
    public AuthenticatedUser withoutCredentials() {
        return new AuthenticatedUser(id, username, fullName, role, null, active);
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority(role.authority())); }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isEnabled() { return active; }
}
