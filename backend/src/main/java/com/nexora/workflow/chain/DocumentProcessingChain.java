package com.nexora.workflow.chain;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Assembles the chain from all {@link DocumentCheckHandler} beans in {@code @Order} order
 * (Spring injects ordered lists) and exposes a single {@link #review} entry point.
 */
@Component
public class DocumentProcessingChain {

    private final DocumentCheckHandler head;
    private final List<String> handlerNames;

    public DocumentProcessingChain(List<DocumentCheckHandler> handlers) {
        if (handlers.isEmpty()) {
            throw new IllegalStateException("Document chain needs at least one handler");
        }
        this.head = handlers.getFirst();
        DocumentCheckHandler current = head;
        for (int i = 1; i < handlers.size(); i++) {
            current = current.linkWith(handlers.get(i));
        }
        this.handlerNames = handlers.stream().map(DocumentCheckHandler::name).toList();
    }

    public DocumentReview review(DocumentReview review) {
        head.handle(review);
        return review;
    }

    public List<String> handlerNames() {
        return handlerNames;
    }
}
