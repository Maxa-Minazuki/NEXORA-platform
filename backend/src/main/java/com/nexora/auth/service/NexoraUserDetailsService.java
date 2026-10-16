package com.nexora.auth.service;

import com.nexora.auth.repository.UserRepository;
import com.nexora.shared.security.AuthenticatedUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NexoraUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public NexoraUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return users.findByUsernameIgnoreCase(username)
                .map(u -> new AuthenticatedUser(u.getId(), u.getUsername(), u.getFullName(), u.getRole(),
                        u.getPasswordHash(), u.isActive()))
                .orElseThrow(() -> new UsernameNotFoundException("Пайдаланушы табылмады"));
    }
}
