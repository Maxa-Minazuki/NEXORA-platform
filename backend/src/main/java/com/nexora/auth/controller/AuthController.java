package com.nexora.auth.controller;

import com.nexora.auth.dto.AuthDtos.CsrfResponse;
import com.nexora.auth.dto.AuthDtos.DemoAccount;
import com.nexora.auth.dto.AuthDtos.LoginRequest;
import com.nexora.auth.dto.AuthDtos.UserProfile;
import com.nexora.auth.service.AuthService;
import com.nexora.auth.service.LoginAttemptService;
import com.nexora.shared.command.AuditTrail;
import com.nexora.shared.exception.ApiError;
import com.nexora.shared.exception.GlobalExceptionHandler;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.shared.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Session based authentication for the SPA. The SPA first calls {@code GET /api/auth/csrf}, then
 * posts credentials as JSON. On success the session id is rotated (fixation protection), the CSRF
 * token is regenerated and the security context is stored in the HTTP session.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final LoginAttemptService loginAttempts;
    private final AuthService authService;
    private final AuditTrail auditTrail;

    public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository,
                          SessionAuthenticationStrategy sessionStrategy, LoginAttemptService loginAttempts,
                          AuthService authService, AuditTrail auditTrail) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionStrategy = sessionStrategy;
        this.loginAttempts = loginAttempts;
        this.authService = authService;
        this.auditTrail = auditTrail;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @GetMapping("/demo-accounts")
    public List<DemoAccount> demoAccounts() {
        return AuthService.DEMO_ACCOUNTS;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        String key = request.username().toLowerCase() + "|" + httpRequest.getRemoteAddr();
        if (loginAttempts.isBlocked(key)) {
            return GlobalExceptionHandler.build(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS",
                    "Сәтсіз әрекеттер тым көп. Бірнеше минуттан кейін қайталап көріңіз.", httpRequest, List.of());
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            loginAttempts.recordFailure(key);
            auditTrail.record(null, truncate(request.username()), "LOGIN", "User", null,
                    AuditTrail.Outcome.FAILURE, "Қате тіркелгі деректері, IP " + httpRequest.getRemoteAddr());
            ResponseEntity<ApiError> body = GlobalExceptionHandler.build(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS",
                    "Пайдаланушы аты немесе құпиясөз қате", httpRequest, List.of());
            return body;
        }
        loginAttempts.recordSuccess(key);
        AuthenticatedUser principal = ((AuthenticatedUser) authentication.getPrincipal()).withoutCredentials();
        Authentication stored = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());

        sessionStrategy.onAuthentication(stored, httpRequest, httpResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(stored);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        auditTrail.record(principal.id(), principal.getUsername(), "LOGIN", "User", String.valueOf(principal.id()),
                AuditTrail.Outcome.SUCCESS, "Жүйеге кірді");
        return ResponseEntity.ok(authService.profile(principal.id()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CurrentUser.find().ifPresent(u -> auditTrail.record(u.id(), u.getUsername(), "LOGOUT", "User",
                String.valueOf(u.id()), AuditTrail.Outcome.SUCCESS, "Жүйеден шықты"));
        new SecurityContextLogoutHandler().logout(request, response, auth);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserProfile me() {
        return authService.profile(CurrentUser.require().id());
    }

    private static String truncate(String s) {
        return s.length() > 50 ? s.substring(0, 50) : s;
    }
}
