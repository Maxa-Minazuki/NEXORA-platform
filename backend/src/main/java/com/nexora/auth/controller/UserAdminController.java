package com.nexora.auth.controller;

import com.nexora.auth.dto.AuthDtos.UpdateUserRequest;
import com.nexora.auth.dto.AuthDtos.UserSummary;
import com.nexora.auth.service.UserAdminService;
import com.nexora.shared.command.Command;
import com.nexora.shared.command.CommandInvoker;
import com.nexora.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdminService service;
    private final CommandInvoker invoker;

    public UserAdminController(UserAdminService service, CommandInvoker invoker) {
        this.service = service;
        this.invoker = invoker;
    }

    @GetMapping
    public List<UserSummary> list() {
        return service.list();
    }

    @PutMapping("/{id}")
    public UserSummary update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        Long actorId = CurrentUser.require().id();
        return invoker.run(new Command<>() {
            @Override public String name() { return "UPDATE_USER"; }
            @Override public String entityType() { return "User"; }
            @Override public UserSummary execute() { return service.update(actorId, id, request); }
            @Override public String entityId(UserSummary r) { return String.valueOf(id); }
            @Override public String details() { return "рөл=" + request.role() + ", белсенді=" + request.active(); }
        });
    }
}
