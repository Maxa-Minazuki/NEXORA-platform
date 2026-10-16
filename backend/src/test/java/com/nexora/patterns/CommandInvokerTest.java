package com.nexora.patterns;

import com.nexora.shared.command.AuditTrail;
import com.nexora.shared.command.Command;
import com.nexora.shared.command.CommandInvoker;
import com.nexora.support.TestTransactionManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Command pattern: the invoker runs any command in a transaction and audits its outcome. */
class CommandInvokerTest {

    private final AuditTrail audit = mock(AuditTrail.class);
    private final TestTransactionManager tx = new TestTransactionManager();
    private final CommandInvoker invoker = new CommandInvoker(tx, audit);

    private Command<Integer> command(boolean fail) {
        return new Command<>() {
            @Override public String name() { return "DOUBLE_IT"; }
            @Override public String entityType() { return "Number"; }
            @Override public Integer execute() {
                if (fail) {
                    throw new IllegalStateException("boom");
                }
                return 21 * 2;
            }
            @Override public String entityId(Integer result) { return result == null ? null : String.valueOf(result); }
            @Override public String details() { return "demo"; }
        };
    }

    @Test
    void successfulCommandCommitsAndIsAudited() {
        assertThat(invoker.run(command(false))).isEqualTo(42);
        assertThat(tx.commits).isEqualTo(1);
        verify(audit).record(isNull(), eq("system"), eq("DOUBLE_IT"), eq("Number"), eq("42"),
                eq(AuditTrail.Outcome.SUCCESS), eq("demo"));
    }

    @Test
    void failingCommandRollsBackAndIsAuditedAsFailure() {
        assertThatThrownBy(() -> invoker.run(command(true))).hasMessage("boom");
        assertThat(tx.rollbacks).isEqualTo(1);
        assertThat(tx.commits).isZero();
        verify(audit).record(isNull(), anyString(), eq("DOUBLE_IT"), eq("Number"), isNull(),
                eq(AuditTrail.Outcome.FAILURE), contains("boom"));
    }
}
