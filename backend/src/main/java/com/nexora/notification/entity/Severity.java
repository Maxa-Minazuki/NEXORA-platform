package com.nexora.notification.entity;

public enum Severity {
    INFO, SUCCESS, WARNING, CRITICAL;

    public boolean atLeast(Severity other) {
        return rank() >= other.rank();
    }

    /** INFO and SUCCESS share the lowest rank: success messages are informational. */
    private int rank() {
        return switch (this) {
            case INFO, SUCCESS -> 0;
            case WARNING -> 1;
            case CRITICAL -> 2;
        };
    }
}
