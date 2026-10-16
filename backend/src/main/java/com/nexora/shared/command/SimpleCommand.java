package com.nexora.shared.command;

import java.util.function.Function;
import java.util.function.Supplier;

/** Small ad-hoc command for simple actions that do not justify a dedicated class but must be audited. */
public final class SimpleCommand<R> implements Command<R> {

    private final String name;
    private final String entityType;
    private final Supplier<R> action;
    private final Function<R, Object> idExtractor;
    private final String details;

    private SimpleCommand(String name, String entityType, Supplier<R> action, Function<R, Object> idExtractor,
                          String details) {
        this.name = name;
        this.entityType = entityType;
        this.action = action;
        this.idExtractor = idExtractor;
        this.details = details;
    }

    public static <R> SimpleCommand<R> of(String name, String entityType, Supplier<R> action,
                                          Function<R, Object> idExtractor, String details) {
        return new SimpleCommand<>(name, entityType, action, idExtractor, details);
    }

    @Override public String name() { return name; }
    @Override public String entityType() { return entityType; }
    @Override public R execute() { return action.get(); }
    @Override public String details() { return details == null ? "" : details; }

    @Override
    public String entityId(R result) {
        if (result == null) {
            return null;
        }
        Object id = idExtractor.apply(result);
        return id == null ? null : id.toString();
    }
}
