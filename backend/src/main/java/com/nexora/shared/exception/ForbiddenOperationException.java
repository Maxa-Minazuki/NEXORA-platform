package com.nexora.shared.exception;

import org.springframework.http.HttpStatus;

/** The caller is authenticated but may not perform this operation on this particular object. */
public class ForbiddenOperationException extends NexoraException {
    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
