package com.nexora.shared.command;

import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.shared.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <h2>Design pattern: Command (invoker)</h2>
 *
 * Executes any {@link Command} inside its own transaction and writes exactly one audit entry with
 * the outcome. Failures are audited too: the audit entry is written after the business transaction
 * has rolled back, through a separate transaction, so a rejected action is still traceable.
 *
 * <p>Controllers/facades call {@code invoker.run(command)}; they never open their own transaction
 * around it, which keeps the invoker the single transaction boundary for user actions.</p>
 */
@Component
public class CommandInvoker {

    private static final Logger log = LoggerFactory.getLogger(CommandInvoker.class);

    private final TransactionTemplate transactionTemplate;
    private final AuditTrail auditTrail;

    public CommandInvoker(PlatformTransactionManager transactionManager, AuditTrail auditTrail) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.auditTrail = auditTrail;
    }

    public <R> R run(Command<R> command) {
        AuthenticatedUser actor = CurrentUser.find().orElse(null);
        Long actorId = actor == null ? null : actor.id();
        String actorName = actor == null ? "system" : actor.getUsername();
        try {
            R result = transactionTemplate.execute(status -> command.execute());
            auditTrail.record(actorId, actorName, command.name(), command.entityType(),
                    command.entityId(result), AuditTrail.Outcome.SUCCESS, command.details());
            return result;
        } catch (RuntimeException ex) {
            log.debug("Command {} failed: {}", command.name(), ex.getMessage());
            auditTrail.record(actorId, actorName, command.name(), command.entityType(),
                    command.entityId(null), AuditTrail.Outcome.FAILURE, truncate(command.details() + " | " + ex.getMessage()));
            throw ex;
        }
    }

    private static String truncate(String s) {
        return s.length() > 990 ? s.substring(0, 990) : s;
    }
}
