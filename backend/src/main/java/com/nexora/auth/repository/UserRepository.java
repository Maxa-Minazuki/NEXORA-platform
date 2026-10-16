package com.nexora.auth.repository;

import com.nexora.auth.entity.User;
import com.nexora.shared.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    List<User> findByRoleInAndActiveTrue(Collection<Role> roles);

    List<User> findAllByOrderByRoleAscFullNameAsc();
}
