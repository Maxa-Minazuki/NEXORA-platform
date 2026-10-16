package com.nexora.shared.exception;

import org.springframework.http.HttpStatus;

/** The request conflicts with the current state of a resource (409), e.g. a taken slot or seat. */
public class ConflictException extends NexoraException {
    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
