package com.bank.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Immutable record of a single banking transaction.
 *
 * <p>Built using the Builder pattern to ensure all required fields are set
 * before the object is created.  Once constructed, no field can be changed,
 * guaranteeing an audit-proof transaction log.</p>
 *
 * @author BankingSystem
 * @version 1.0
 */
public final class Transaction implements Serializable, Comparable<Transaction> {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    // ── Core fields ────────────────────────────────────────────────────────────
    private final String          transactionId;
    private final String          accountNumber;
    private final TransactionType type;
    private final double          amount;
    private final double          balanceAfter;
    private final LocalDateTime   timestamp;
    private final String          description;
    private final String          referenceId;   // e.g. target account for transfers

    // ── Private constructor – use Builder ──────────────────────────────────────
    private Transaction(Builder builder) {
        this.transactionId = builder.transactionId;
        this.accountNumber = builder.accountNumber;
        this.type          = builder.type;
        this.amount        = builder.amount;
        this.balanceAfter  = builder.balanceAfter;
        this.timestamp     = builder.timestamp;
        this.description   = builder.description;
        this.referenceId   = builder.referenceId;
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public String          getTransactionId() { return transactionId; }
    public String          getAccountNumber() { return accountNumber; }
    public TransactionType getType()          { return type; }
    public double          getAmount()        { return amount; }
    public double          getBalanceAfter()  { return balanceAfter; }
    public LocalDateTime   getTimestamp()     { return timestamp; }
    public String          getDescription()   { return description; }
    public String          getReferenceId()   { return referenceId; }

    @Override
    public int compareTo(Transaction other) {
        return this.timestamp.compareTo(other.timestamp);
    }

    @Override
    public String toString() {
        String sign = type.isCredit() ? "+" : "-";
        return String.format("[%s] %s | %s | %s₹%,.2f | Balance: ₹%,.2f | %s",
                timestamp.format(FORMATTER),
                transactionId,
                type.getDisplayName(),
                sign, amount,
                balanceAfter,
                description);
    }

    // ── Builder ────────────────────────────────────────────────────────────────
    /**
     * Fluent builder for constructing {@link Transaction} objects.
     *
     * <p>Mandatory fields: {@code accountNumber}, {@code type}, {@code amount},
     * {@code balanceAfter}.</p>
     */
    public static class Builder {
        private String          transactionId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        private String          accountNumber;
        private TransactionType type;
        private double          amount;
        private double          balanceAfter;
        private LocalDateTime   timestamp     = LocalDateTime.now();
        private String          description   = "";
        private String          referenceId   = null;

        public Builder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber; return this;
        }
        public Builder type(TransactionType type) {
            this.type = type; return this;
        }
        public Builder amount(double amount) {
            this.amount = amount; return this;
        }
        public Builder balanceAfter(double balanceAfter) {
            this.balanceAfter = balanceAfter; return this;
        }
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp; return this;
        }
        public Builder description(String description) {
            this.description = description; return this;
        }
        public Builder referenceId(String referenceId) {
            this.referenceId = referenceId; return this;
        }

        public Transaction build() {
            if (accountNumber == null || type == null)
                throw new IllegalStateException("accountNumber and type are mandatory");
            if (amount <= 0)
                throw new IllegalStateException("Transaction amount must be positive");
            return new Transaction(this);
        }
    }
}
