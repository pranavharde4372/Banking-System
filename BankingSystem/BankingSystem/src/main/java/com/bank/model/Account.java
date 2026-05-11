package com.bank.model;

import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidTransactionException;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class for all bank account types.
 *
 * <p>Encapsulates the shared state (balance, transaction history, status) and
 * enforces the Template Method pattern: concrete subclasses implement
 * {@link #validateWithdrawal(double)} and {@link #getAccountDetails()} to
 * provide type-specific behaviour.</p>
 *
 * <p>Thread safety: balance mutations are {@code synchronized} to allow
 * concurrent transaction processing without data corruption.</p>
 *
 * @author BankingSystem
 * @version 1.0
 */
public abstract class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Identity ───────────────────────────────────────────────────────────────
    protected final String      accountNumber;
    protected final String      customerId;
    protected final AccountType accountType;
    protected final LocalDateTime openedOn;

    // ── Financials ─────────────────────────────────────────────────────────────
    protected volatile double balance;
    protected double interestEarned;

    // ── Status ─────────────────────────────────────────────────────────────────
    protected boolean active;
    protected boolean frozen;

    // ── History ────────────────────────────────────────────────────────────────
    protected final List<Transaction> transactions;

    // ── Constructor ────────────────────────────────────────────────────────────
    protected Account(String accountNumber, String customerId,
                      AccountType accountType, double initialDeposit) {
        if (initialDeposit < accountType.getMinimumBalance())
            throw new InvalidTransactionException(
                    String.format("Initial deposit ₹%.2f is below minimum ₹%.2f for %s",
                            initialDeposit, accountType.getMinimumBalance(), accountType.getDisplayName()),
                    accountNumber);

        this.accountNumber  = Objects.requireNonNull(accountNumber, "Account number required");
        this.customerId     = Objects.requireNonNull(customerId,    "Customer ID required");
        this.accountType    = Objects.requireNonNull(accountType,   "Account type required");
        this.openedOn       = LocalDateTime.now();
        this.balance        = initialDeposit;
        this.interestEarned = 0.0;
        this.active         = true;
        this.frozen         = false;
        this.transactions   = new ArrayList<>();
    }

    // ── Abstract methods (Template Method pattern) ─────────────────────────────
    /**
     * Subclasses validate type-specific withdrawal rules
     * (e.g. minimum balance, overdraft limits).
     *
     * @param amount positive withdrawal amount
     * @throws InsufficientFundsException   if funds are insufficient
     * @throws InvalidTransactionException  if type rules are violated
     */
    protected abstract void validateWithdrawal(double amount);

    /** @return a multi-line string of type-specific account details */
    public abstract String getAccountDetails();

    // ── Core operations ────────────────────────────────────────────────────────
    /**
     * Credits the account with {@code amount}.
     *
     * @param amount      positive value
     * @param description human-readable reason
     * @return the created {@link Transaction}
     */
    public synchronized Transaction deposit(double amount, String description) {
        validateActive();
        if (amount <= 0)
            throw new InvalidTransactionException("Deposit amount must be positive", accountNumber);

        balance += amount;
        Transaction tx = new Transaction.Builder()
                .accountNumber(accountNumber)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceAfter(balance)
                .description(description)
                .build();
        transactions.add(tx);
        return tx;
    }

    /**
     * Debits the account by {@code amount} after validation.
     *
     * @param amount      positive value
     * @param description human-readable reason
     * @return the created {@link Transaction}
     */
    public synchronized Transaction withdraw(double amount, String description) {
        validateActive();
        if (amount <= 0)
            throw new InvalidTransactionException("Withdrawal amount must be positive", accountNumber);
        validateWithdrawal(amount);

        balance -= amount;
        Transaction tx = new Transaction.Builder()
                .accountNumber(accountNumber)
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .balanceAfter(balance)
                .description(description)
                .build();
        transactions.add(tx);
        return tx;
    }

    /** Adds a pre-built transaction to the history (used by transfer and interest). */
    public synchronized void addTransaction(Transaction tx) {
        transactions.add(tx);
    }

    /** Applies computed interest directly to the balance. */
    public synchronized void applyInterest(double interest, String description) {
        if (interest <= 0) return;
        balance += interest;
        interestEarned += interest;
        Transaction tx = new Transaction.Builder()
                .accountNumber(accountNumber)
                .type(TransactionType.INTEREST_CREDIT)
                .amount(interest)
                .balanceAfter(balance)
                .description(description)
                .build();
        transactions.add(tx);
    }

    // ── Validation helpers ─────────────────────────────────────────────────────
    protected void validateActive() {
        if (!active)  throw new InvalidTransactionException("Account is closed", accountNumber);
        if (frozen)   throw new InvalidTransactionException("Account is frozen", accountNumber);
    }

    protected void checkSufficientFunds(double amount) {
        if (balance < amount)
            throw new InsufficientFundsException(accountNumber, amount, balance);
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public String      getAccountNumber()  { return accountNumber; }
    public String      getCustomerId()     { return customerId; }
    public AccountType getAccountType()    { return accountType; }
    public LocalDateTime getOpenedOn()     { return openedOn; }
    public double      getBalance()        { return balance; }
    public double      getInterestEarned() { return interestEarned; }
    public boolean     isActive()          { return active; }
    public boolean     isFrozen()          { return frozen; }
    public List<Transaction> getTransactions() { return Collections.unmodifiableList(transactions); }
    public int         getTransactionCount()   { return transactions.size(); }

    // ── Mutators ───────────────────────────────────────────────────────────────
    public void setActive(boolean active) { this.active = active; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    // ── Utility ────────────────────────────────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        return Objects.equals(accountNumber, ((Account) o).accountNumber);
    }

    @Override
    public int hashCode() { return Objects.hash(accountNumber); }

    @Override
    public String toString() {
        return String.format("Account[%s | %s | Balance: ₹%,.2f | %s]",
                accountNumber, accountType.getDisplayName(),
                balance, active ? "ACTIVE" : "CLOSED");
    }
}
