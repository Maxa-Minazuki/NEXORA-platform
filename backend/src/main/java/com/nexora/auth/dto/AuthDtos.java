package com.nexora.auth.dto;

import com.nexora.shared.security.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Size(max = 50) String username,
                               @NotBlank @Size(max = 100) String password) {
    }

    public record UserProfile(Long id, String username, String fullName, String email, Role role, String title) {
    }

    public record DemoAccount(String username, Role role, String fullName, String description) {
    }

    public record CsrfResponse(String headerName, String token) {
    }

    public record UserSummary(Long id, String username, String fullName, String email, Role role, String title,
                              boolean active, Instant createdAt) {
    }

    public record UpdateUserRequest(@NotNull Role role, @NotNull Boolean active) {
    }
}
