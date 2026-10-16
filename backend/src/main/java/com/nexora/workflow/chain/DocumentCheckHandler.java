package com.nexora.workflow.chain;

/**
 * <h2>Design pattern: Chain of Responsibility</h2>
 *
 * Abstract handler. Each concrete handler performs one check on the {@link DocumentReview}; it
 * either lets the review continue to the next handler or stops the chain with a clear reason.
 * Handlers are Spring beans ordered with {@code @Order}; {@link DocumentProcessingChain} links them,
 * so a new rule is added by writing one new {@code @Component} - nothing else changes.
 */
public abstract class DocumentCheckHandler {

    private DocumentCheckHandler next;

    final DocumentCheckHandler linkWith(DocumentCheckHandler next) {
        this.next = next;
        return next;
    }

    public final void handle(DocumentReview review) {
        check(review);
        if (!review.stopped() && next != null) {
            next.handle(review);
        }
    }

    /** Perform the check; call {@code review.stop(...)} to break the chain. */
    protected abstract void check(DocumentReview review);

    public abstract String name();

    protected void pass(DocumentReview review, String message) {
        review.record(name(), DocumentReview.Outcome.PASSED, message);
    }

    protected void fail(DocumentReview review, String reason) {
        review.stop(name(), reason);
    }
}
