package com.nexora.audit.controller;

import com.nexora.audit.dto.AuditLogDto;
import com.nexora.audit.repository.AuditLogRepository;
import com.nexora.shared.web.PageRequests;
import com.nexora.shared.web.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AuditController {

    private final AuditLogRepository repository;

    public AuditController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<AuditLogDto> search(@RequestParam(required = false) String action,
                                            @RequestParam(required = false) String entityType,
                                            @RequestParam(required = false) String outcome,
                                            @RequestParam(required = false) String actor,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequests.of(page, size, null, Set.of(), "-createdAt");
        return PageResponse.of(repository.search(blankToNull(action), blankToNull(entityType), blankToNull(outcome),
                blankToNull(actor), pageable), AuditLogDto::from);
    }

    @GetMapping("/actions")
    public List<String> actions() {
        return repository.distinctActions();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
