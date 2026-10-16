package com.nexora.shared.exception;

import org.springframework.http.HttpStatus;

/** A request that is well-formed but violates a business rule (422). */
public class BusinessRuleException extends NexoraException {
    public BusinessRuleException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
