package com.nexora.support;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Minimal transaction manager for unit tests of transaction-aware code without a database. */
public class TestTransactionManager implements PlatformTransactionManager {

    public int commits;
    public int rollbacks;

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
        return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) {
        commits++;
    }

    @Override
    public void rollback(TransactionStatus status) {
        rollbacks++;
    }
}
