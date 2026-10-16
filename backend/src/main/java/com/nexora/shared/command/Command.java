package com.nexora.shared.command;

/**
 * <h2>Design pattern: Command</h2>
 *
 * A user action encapsulated as an object: it knows its receiver (the service that does the real
 * work), carries all parameters of the request and can be executed later by the
 * {@link CommandInvoker}. Because every action has the same shape, the invoker can wrap all of them
 * with the same cross-cutting behaviour: a transaction boundary and an audit-log entry with the
 * outcome.
 *
 * <p>Undo is intentionally not part of the contract: approving a document, sending a notification
 * or releasing a seat cannot be safely "un-done" under the business rules, so no fake undo is
 * offered.</p>
 *
 * @param <R> result type returned to the caller
 */
public interface Command<R> {

    /** Stable action name written to the audit log, e.g. {@code APPROVE_DOCUMENT}. */
    String name();

    /** Entity type the command acts on, e.g. {@code Document}. */
    String entityType();

    /** Performs the action against the receiver. Runs inside the invoker's transaction. */
    R execute();

    /** Id of the affected entity, available after execution (creation commands learn it from the result). */
    default String entityId(R result) {
        return null;
    }

    /** Human readable details for the audit log. */
    default String details() {
        return "";
    }
}
