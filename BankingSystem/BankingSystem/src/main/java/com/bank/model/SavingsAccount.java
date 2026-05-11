package com.bank.model;

import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidTransactionException;

/**
 * Savings account: earns 4% p.a. interest and maintains a minimum balance.
 *
 * <p>Withdrawal is blocked if the resulting balance would fall below the
 * {@link AccountType#SAVINGS} minimum balance of ₹500.</p>
 */
public class SavingsAccount extends Account {

    private static final double MINIMUM_BALANCE = AccountType.SAVINGS.getMinimumBalance();
    private static final int    MAX_WITHDRAWALS_PER_MONTH = 6;

    private int withdrawalsThisMonth;
    private int currentMonth;

    public SavingsAccount(String accountNumber, String customerId, double initialDeposit) {
        super(accountNumber, customerId, AccountType.SAVINGS, initialDeposit);
        this.withdrawalsThisMonth = 0;
        this.currentMonth = java.time.LocalDate.now().getMonthValue();
    }

    @Override
    protected void validateWithdrawal(double amount) {
        resetMonthlyCounterIfNeeded();

        if (withdrawalsThisMonth >= MAX_WITHDRAWALS_PER_MONTH)
            throw new InvalidTransactionException(
                    "Monthly withdrawal limit of " + MAX_WITHDRAWALS_PER_MONTH + " reached",
                    accountNumber);

        double projected = balance - amount;
        if (projected < MINIMUM_BALANCE)
            throw new InsufficientFundsException(accountNumber, amount, balance - MINIMUM_BALANCE);
    }

    @Override
    public synchronized Transaction withdraw(double amount, String description) {
        resetMonthlyCounterIfNeeded();
        Transaction tx = super.withdraw(amount, description);
        withdrawalsThisMonth++;
        return tx;
    }

    private void resetMonthlyCounterIfNeeded() {
        int month = java.time.LocalDate.now().getMonthValue();
        if (month != currentMonth) {
            withdrawalsThisMonth = 0;
            currentMonth = month;
        }
    }

    public int getRemainingWithdrawals() {
        resetMonthlyCounterIfNeeded();
        return MAX_WITHDRAWALS_PER_MONTH - withdrawalsThisMonth;
    }

    @Override
    public String getAccountDetails() {
        return String.format(
                "  Type             : Savings Account%n" +
                "  Interest Rate    : %.1f%% p.a.%n" +
                "  Minimum Balance  : ₹%,.2f%n" +
                "  Withdrawals Left : %d / %d this month%n" +
                "  Interest Earned  : ₹%,.2f",
                AccountType.SAVINGS.getInterestRate(),
                MINIMUM_BALANCE,
                getRemainingWithdrawals(),
                MAX_WITHDRAWALS_PER_MONTH,
                interestEarned);
    }
}
