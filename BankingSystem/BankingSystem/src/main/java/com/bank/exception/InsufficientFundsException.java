package com.bank.exception;

/**
 * Thrown when a withdrawal or transfer exceeds the available balance.
 */
public class InsufficientFundsException extends BankingException {
    private final double requested;
    private final double available;

    public InsufficientFundsException(String accountNumber, double requested, double available) {
        super(String.format("Insufficient funds: requested ₹%.2f but only ₹%.2f available",
                requested, available), "INSUFFICIENT_FUNDS", accountNumber);
        this.requested = requested;
        this.available = available;
    }

    public double getRequested() { return requested; }
    public double getAvailable() { return available; }
    public double getShortfall()  { return requested - available; }
}
