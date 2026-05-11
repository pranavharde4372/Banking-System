package com.bank.factory;

import com.bank.model.*;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory for creating {@link Account} instances.
 *
 * <p>Implements the <strong>Factory Method</strong> design pattern:
 * client code never invokes {@code new SavingsAccount(…)} directly; it
 * calls the factory, keeping construction logic in one place.  The factory
 * also auto-generates unique, sequential account numbers.</p>
 *
 * <p>Implemented as a Singleton so that the account-number counter is
 * shared across the entire JVM.</p>
 */
public final class AccountFactory {

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static final AccountFactory INSTANCE = new AccountFactory();
    private AccountFactory() {}
    public static AccountFactory getInstance() { return INSTANCE; }

    // ── Account number generator ───────────────────────────────────────────────
    private final AtomicInteger counter = new AtomicInteger(1000);

    private String nextAccountNumber(String prefix) {
        return prefix + "-" + counter.incrementAndGet();
    }

    // ── Factory methods ────────────────────────────────────────────────────────

    /**
     * Creates a {@link SavingsAccount}.
     *
     * @param customerId     owner's customer ID
     * @param initialDeposit opening balance (≥ ₹500)
     * @return new SavingsAccount
     */
    public SavingsAccount createSavingsAccount(String customerId, double initialDeposit) {
        String accNo = nextAccountNumber("SAV");
        return new SavingsAccount(accNo, customerId, initialDeposit);
    }

    /**
     * Creates a {@link CurrentAccount} with a default overdraft limit of ₹5,000.
     */
    public CurrentAccount createCurrentAccount(String customerId, double initialDeposit) {
        return createCurrentAccount(customerId, initialDeposit, 5000.0);
    }

    /**
     * Creates a {@link CurrentAccount} with a specified overdraft limit.
     */
    public CurrentAccount createCurrentAccount(String customerId,
                                               double initialDeposit,
                                               double overdraftLimit) {
        String accNo = nextAccountNumber("CUR");
        return new CurrentAccount(accNo, customerId, initialDeposit, overdraftLimit);
    }

    /**
     * Creates a {@link FixedDepositAccount}.
     *
     * @param customerId   owner's customer ID
     * @param principal    amount locked in (≥ ₹10,000)
     * @param tenureMonths investment period
     * @return new FixedDepositAccount
     */
    public FixedDepositAccount createFixedDeposit(String customerId,
                                                  double principal,
                                                  int tenureMonths) {
        String accNo = nextAccountNumber("FD");
        return new FixedDepositAccount(accNo, customerId, principal, tenureMonths);
    }

    /**
     * Generic factory method dispatching on {@link AccountType}.
     *
     * @param type           desired account type (SAVINGS / CURRENT / FIXED_DEPOSIT)
     * @param customerId     owner
     * @param initialDeposit opening balance
     * @return appropriate Account subclass
     */
    public Account createAccount(AccountType type, String customerId, double initialDeposit) {
        return switch (type) {
            case SAVINGS       -> createSavingsAccount(customerId, initialDeposit);
            case CURRENT       -> createCurrentAccount(customerId, initialDeposit);
            case FIXED_DEPOSIT -> createFixedDeposit(customerId, initialDeposit, 12);
            default -> throw new IllegalArgumentException("Unsupported account type: " + type);
        };
    }
}
