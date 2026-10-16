package com.nexora.auth.service;

import com.nexora.auth.dto.AuthDtos.UpdateUserRequest;
import com.nexora.auth.dto.AuthDtos.UserSummary;
import com.nexora.auth.entity.User;
import com.nexora.auth.repository.UserRepository;
import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAdminService {

    private final UserRepository users;

    public UserAdminService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<UserSummary> list() {
        return users.findAllByOrderByRoleAscFullNameAsc().stream().map(UserAdminService::toSummary).toList();
    }

    /** Receiver of {@code UpdateUserCommand}. Admins cannot lock themselves out. */
    @Transactional
    public UserSummary update(Long actorId, Long userId, UpdateUserRequest request) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
        if (user.getId().equals(actorId) && (!request.active() || request.role() != user.getRole())) {
            throw new BusinessRuleException("SELF_LOCKOUT", "Өзіңізді бұғаттай немесе өз рөліңізді өзгерте алмайсыз");
        }
        user.changeRole(request.role());
        user.setActive(request.active());
        return toSummary(user);
    }

    static UserSummary toSummary(User u) {
        return new UserSummary(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole(), u.getTitle(),
                u.isActive(), u.getCreatedAt());
    }
}
