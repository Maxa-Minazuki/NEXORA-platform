package com.nexora.shared.exception;

import com.nexora.shared.i18n.Kk;
import org.springframework.http.HttpStatus;

/** Thrown by State-pattern objects when an operation is not allowed in the current state. */
public class InvalidStateTransitionException extends NexoraException {
    public InvalidStateTransitionException(String entity, String state, String operation) {
        super(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION",
                "«" + Kk.status(state) + "» күйіндегі " + Kk.entityAccusative(entity) + " " + Kk.operation(operation)
                        + " мүмкін емес");
    }
}
