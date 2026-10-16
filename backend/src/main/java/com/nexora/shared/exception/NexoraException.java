package com.nexora.shared.exception;

import org.springframework.http.HttpStatus;

/** Base class of all business exceptions; carries the HTTP status and a stable machine code. */
public abstract class NexoraException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected NexoraException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
}
