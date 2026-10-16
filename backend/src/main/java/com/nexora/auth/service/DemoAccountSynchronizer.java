package com.nexora.auth.service;

import com.nexora.auth.entity.User;
import com.nexora.auth.repository.UserRepository;
import com.nexora.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Keeps the five local demo accounts usable on every start: if an account is missing it is created,
 * if its password no longer matches the documented demo password it is re-hashed with BCrypt, and a
 * deactivated or re-assigned demo account gets its original role back. Only these five usernames are
 * touched; no business data is deleted and no duplicates are created (lookup by username).
 * Disable with {@code DEMO_ACCOUNTS_SYNC=false}.
 */
@Component
public class DemoAccountSynchronizer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountSynchronizer.class);

    record Demo(String username, String password, Role role, String fullName, String email, String title) {
    }

    static final List<Demo> DEMO = List.of(
            new Demo("admin", "Admin#2026", Role.ADMIN, "Аружан Садықова", "admin@nexora.local", "Платформа әкімшісі"),
            new Demo("manager", "Manager#2026", Role.MANAGER, "Данияр Беков", "manager@nexora.local", "Операциялық менеджер"),
            new Demo("doctor", "Doctor#2026", Role.DOCTOR, "Айгерім Нұрланова", "doctor@nexora.local", "Кардиолог-дәрігер"),
            new Demo("employee", "Employee#2026", Role.EMPLOYEE, "Тимур Ахметов", "employee@nexora.local", "Қойма және тіркеу маманы"),
            new Demo("user", "User#2026", Role.USER, "Мадина Серікова", "user@nexora.local", "Клиент"));

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final boolean enabled;

    public DemoAccountSynchronizer(UserRepository users, PasswordEncoder encoder,
                                   @Value("${nexora.demo-accounts-sync:true}") boolean enabled) {
        this.users = users;
        this.encoder = encoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        int created = 0;
        int repaired = 0;
        for (Demo d : DEMO) {
            var existing = users.findByUsernameIgnoreCase(d.username());
            if (existing.isEmpty()) {
                users.save(new User(d.username(), d.email(), d.fullName(), encoder.encode(d.password()), d.role(), d.title()));
                created++;
                continue;
            }
            User u = existing.get();
            boolean changed = false;
            if (!encoder.matches(d.password(), u.getPasswordHash())) {
                u.changePasswordHash(encoder.encode(d.password()));
                changed = true;
            }
            if (!u.isActive()) {
                u.setActive(true);
                changed = true;
            }
            if (u.getRole() != d.role()) {
                u.changeRole(d.role());
                changed = true;
            }
            if (changed) {
                repaired++;
            }
        }
        if (created + repaired > 0) {
            log.info("Demo accounts synchronized: {} created, {} repaired", created, repaired);
        }
    }
}
