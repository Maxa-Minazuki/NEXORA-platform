package com.nexora.lab;

import com.nexora.audit.dto.AuditLogDto;
import com.nexora.audit.repository.AuditLogRepository;
import com.nexora.shared.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Backend of the "Design Patterns Lab" page: each endpoint runs a real scenario. */
@RestController
@RequestMapping("/api/lab")
public class LabController {

    private final LabService lab;
    private final AuditLogRepository audit;

    public LabController(LabService lab, AuditLogRepository audit) {
        this.lab = lab;
        this.audit = audit;
    }

    @GetMapping("/singleton")
    public Map<String, Object> singleton() {
        return lab.singleton();
    }

    @GetMapping("/flight-strategies")
    public Map<String, Object> flightStrategies(@RequestParam(defaultValue = "ALA") String from,
                                                @RequestParam(defaultValue = "NQZ") String to,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return lab.flightStrategies(from, to, date, CurrentUser.require());
    }

    @PostMapping("/low-stock")
    public Map<String, Object> lowStock() {
        return lab.lowStock(CurrentUser.require());
    }

    @PostMapping("/booking-lifecycle")
    public Map<String, Object> bookingLifecycle() {
        return lab.bookingLifecycle(CurrentUser.require());
    }

    @PostMapping("/workflow-submit")
    public Map<String, Object> workflowSubmit(@RequestParam(defaultValue = "1800000.00") BigDecimal amount) {
        return lab.workflowSubmit(CurrentUser.require(), amount);
    }

    /** The Command invoker's audit trail for the current user. */
    @GetMapping("/my-commands")
    @Transactional(readOnly = true)
    public List<AuditLogDto> myCommands() {
        String username = CurrentUser.require().getUsername();
        return audit.search(null, null, null, username, PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream().filter(a -> a.getActorUsername().equalsIgnoreCase(username))
                .map(AuditLogDto::from).toList();
    }
}
