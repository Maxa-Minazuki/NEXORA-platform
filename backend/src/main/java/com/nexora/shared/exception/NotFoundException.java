package com.nexora.shared.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends NexoraException {
    public NotFoundException(String entity, Object id) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", "Сұралған жазба табылмады (" + entity + " #" + id + ")");
    }
}
