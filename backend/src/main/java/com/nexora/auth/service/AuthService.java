package com.nexora.auth.service;

import com.nexora.auth.dto.AuthDtos.UserProfile;
import com.nexora.auth.dto.AuthDtos.DemoAccount;
import com.nexora.auth.entity.User;
import com.nexora.auth.repository.UserRepository;
import com.nexora.shared.exception.NotFoundException;
import com.nexora.shared.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    /**
     * Local demo accounts created by the V2 Flyway migration (passwords stored only as BCrypt hashes).
     * The endpoint deliberately exposes no passwords; they are documented in the README.
     */
    public static final List<DemoAccount> DEMO_ACCOUNTS = List.of(
            new DemoAccount("admin", Role.ADMIN, "Аружан Садықова", "Пайдаланушылар, баптаулар, әрекеттер журналы"),
            new DemoAccount("manager", Role.MANAGER, "Данияр Беков", "Құжаттарды бекітеді, операцияларды басқарады"),
            new DemoAccount("doctor", Role.DOCTOR, "Айгерім Нұрланова", "Өз кестесі мен қабылдаулары"),
            new DemoAccount("employee", Role.EMPLOYEE, "Тимур Ахметов", "Қойма, тіркеу бөлімі, құжаттар"),
            new DemoAccount("user", Role.USER, "Мадина Серікова", "Өз брондаулары, жазылулары, хабарландырулары"));

    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserProfile profile(Long userId) {
        User u = users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
        return new UserProfile(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole(), u.getTitle());
    }
}
