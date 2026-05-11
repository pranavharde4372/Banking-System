package com.bank.model;

/**
 * Enumeration of supported bank account types.
 * Each type carries its own interest rate and minimum balance.
 */
public enum AccountType {
    SAVINGS("Savings Account", 4.0, 500.0),
    CURRENT("Current Account", 0.5, 1000.0),
    FIXED_DEPOSIT("Fixed Deposit", 6.5, 10000.0),
    LOAN("Loan Account", 8.5, 0.0);

    private final String displayName;
    private final double interestRate;   // annual %
    private final double minimumBalance;

    AccountType(String displayName, double interestRate, double minimumBalance) {
        this.displayName    = displayName;
        this.interestRate   = interestRate;
        this.minimumBalance = minimumBalance;
    }

    public String getDisplayName()    { return displayName; }
    public double getInterestRate()   { return interestRate; }
    public double getMinimumBalance() { return minimumBalance; }
}
