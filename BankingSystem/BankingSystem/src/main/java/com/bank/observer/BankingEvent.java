package com.bank.observer;

import com.bank.model.Account;
import com.bank.model.Transaction;

/**
 * Encapsulates a banking event for the Observer pattern.
 *
 * <p>Posted to all registered {@link TransactionObserver}s whenever a
 * noteworthy event (high-value transaction, account freeze, etc.) occurs.</p>
 */
public class BankingEvent {

    public enum EventType {
        TRANSACTION_COMPLETED,
        LARGE_TRANSACTION,          // above threshold
        LOW_BALANCE_WARNING,
        ACCOUNT_FROZEN,
        ACCOUNT_CLOSED,
        LOAN_APPROVED,
        LOAN_DEFAULTED,
        INTEREST_APPLIED,
        SUSPICIOUS_ACTIVITY
    }

    private final EventType   type;
    private final Account     account;
    private final Transaction transaction;   // may be null for non-tx events
    private final String      message;

    public BankingEvent(EventType type, Account account, Transaction transaction, String message) {
        this.type        = type;
        this.account     = account;
        this.transaction = transaction;
        this.message     = message;
    }

    public EventType   getType()        { return type; }
    public Account     getAccount()     { return account; }
    public Transaction getTransaction() { return transaction; }
    public String      getMessage()     { return message; }

    @Override
    public String toString() {
        return String.format("[EVENT:%s] %s | %s",
                type, account.getAccountNumber(), message);
    }
}
