package com.bank.model;

import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidTransactionException;

/**
 * Current (chequing) account with overdraft facility.
 *
 * <p>The account holder may withdraw up to their overdraft limit beyond the
 * current balance.  Interest is charged on the overdrawn amount daily.</p>
 */
public class CurrentAccount extends Account {

    private static final double MINIMUM_BALANCE = AccountType.CURRENT.getMinimumBalance();

    private double overdraftLimit;
    private double overdraftUsed;

    public CurrentAccount(String accountNumber, String customerId,
                          double initialDeposit, double overdraftLimit) {
        super(accountNumber, customerId, AccountType.CURRENT, initialDeposit);
        this.overdraftLimit = Math.max(0, overdraftLimit);
        this.overdraftUsed  = 0.0;
    }

    public CurrentAccount(String accountNumber, String customerId, double initialDeposit) {
        this(accountNumber, customerId, initialDeposit, 5000.0);
    }

    @Override
    protected void validateWithdrawal(double amount) {
        double available = balance + overdraftLimit - overdraftUsed;
        if (amount > available)
            throw new InsufficientFundsException(accountNumber, amount, available);
    }

    @Override
    public synchronized Transaction withdraw(double amount, String description) {
        validateActive();
        if (amount <= 0)
            throw new InvalidTransactionException("Withdrawal amount must be positive", accountNumber);
        validateWithdrawal(amount);

        double fromBalance  = Math.min(amount, balance);
        double fromOverdraft = amount - fromBalance;
        balance        -= fromBalance;
        overdraftUsed  += fromOverdraft;

        Transaction tx = new Transaction.Builder()
                .accountNumber(accountNumber)
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .balanceAfter(balance)
                .description(description + (fromOverdraft > 0
                        ? String.format(" [Overdraft: ₹%.2f]", fromOverdraft) : ""))
                .build();
        addTransaction(tx);
        return tx;
    }

    public double getAvailableBalance()   { return balance + overdraftLimit - overdraftUsed; }
    public double getOverdraftLimit()     { return overdraftLimit; }
    public double getOverdraftUsed()      { return overdraftUsed; }
    public double getOverdraftAvailable() { return overdraftLimit - overdraftUsed; }

    public void setOverdraftLimit(double limit) { this.overdraftLimit = Math.max(0, limit); }

    @Override
    public String getAccountDetails() {
        return String.format(
                "  Type               : Current Account%n" +
                "  Interest Rate      : %.1f%% p.a.%n" +
                "  Minimum Balance    : ₹%,.2f%n" +
                "  Overdraft Limit    : ₹%,.2f%n" +
                "  Overdraft Used     : ₹%,.2f%n" +
                "  Available Balance  : ₹%,.2f",
                AccountType.CURRENT.getInterestRate(),
                MINIMUM_BALANCE,
                overdraftLimit,
                overdraftUsed,
                getAvailableBalance());
    }
}
