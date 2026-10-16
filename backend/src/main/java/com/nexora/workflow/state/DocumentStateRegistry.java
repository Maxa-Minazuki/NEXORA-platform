package com.nexora.workflow.state;

import com.nexora.workflow.entity.Document;
import com.nexora.workflow.entity.DocumentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DocumentStateRegistry {

    private final Map<DocumentStatus, DocumentState> states = new EnumMap<>(DocumentStatus.class);

    public DocumentStateRegistry(List<DocumentState> all) {
        all.forEach(s -> states.put(s.status(), s));
        for (DocumentStatus status : DocumentStatus.values()) {
            if (!states.containsKey(status)) {
                throw new IllegalStateException("No DocumentState implementation for " + status);
            }
        }
    }

    public DocumentState of(Document document) {
        return states.get(document.getStatus());
    }
}
