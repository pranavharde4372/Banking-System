package com.bank.model;

import com.bank.exception.InvalidTransactionException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Fixed Deposit account: locked for a tenure, earns 6.5% p.a.
 *
 * <p>Premature withdrawal is allowed but incurs a 1% penalty on the
 * interest otherwise earned.</p>
 */
public class FixedDepositAccount extends Account {

    private final double    principal;
    private final LocalDate maturityDate;
    private final int       tenureMonths;
    private       boolean   matured;
    private       boolean   prematurelyClosed;

    public FixedDepositAccount(String accountNumber, String customerId,
                               double principal, int tenureMonths) {
        super(accountNumber, customerId, AccountType.FIXED_DEPOSIT, principal);
        this.principal         = principal;
        this.tenureMonths      = tenureMonths;
        this.maturityDate      = LocalDate.now().plusMonths(tenureMonths);
        this.matured           = false;
        this.prematurelyClosed = false;
    }

    @Override
    protected void validateWithdrawal(double amount) {
        if (!matured && !prematurelyClosed)
            throw new InvalidTransactionException(
                    "Fixed deposit has not matured yet. Maturity date: " + maturityDate,
                    accountNumber);
    }

    /** Called by the interest engine when the FD reaches maturity. */
    public void markMatured() {
        this.matured = true;
    }

    /**
     * Computes the payout for a premature closure (principal + reduced interest).
     * @return net amount after 1% penalty on interest
     */
    public double calculatePrematureClosureAmount() {
        long daysElapsed = ChronoUnit.DAYS.between(openedOn.toLocalDate(), LocalDate.now());
        double fullInterest = principal * AccountType.FIXED_DEPOSIT.getInterestRate() / 100.0
                * daysElapsed / 365.0;
        double penalty = fullInterest * 0.01;
        return principal + fullInterest - penalty;
    }

    /** Closes the FD prematurely, applying a penalty. */
    public synchronized Transaction closePremature(String reason) {
        validateActive();
        if (matured) throw new InvalidTransactionException(
                "FD has already matured – use normal withdrawal", accountNumber);

        double payout = calculatePrematureClosureAmount();
        double penalty = (balance - payout);

        prematurelyClosed = true;
        active = false;

        // Record the penalty
        if (penalty > 0) {
            Transaction penaltyTx = new Transaction.Builder()
                    .accountNumber(accountNumber)
                    .type(TransactionType.PENALTY)
                    .amount(penalty)
                    .balanceAfter(payout)
                    .description("Premature closure penalty")
                    .build();
            transactions.add(penaltyTx);
            balance = payout;
        }

        return new Transaction.Builder()
                .accountNumber(accountNumber)
                .type(TransactionType.WITHDRAWAL)
                .amount(payout)
                .balanceAfter(0)
                .description("Premature FD closure: " + reason)
                .build();
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public double    getPrincipal()         { return principal; }
    public LocalDate getMaturityDate()      { return maturityDate; }
    public int       getTenureMonths()      { return tenureMonths; }
    public boolean   isMatured()            { return matured; }
    public boolean   isPrematurelyClosed()  { return prematurelyClosed; }

    public long daysToMaturity() {
        return ChronoUnit.DAYS.between(LocalDate.now(), maturityDate);
    }

    public double projectedMaturityAmount() {
        return principal * (1 + AccountType.FIXED_DEPOSIT.getInterestRate() / 100.0
                * tenureMonths / 12.0);
    }

    @Override
    public String getAccountDetails() {
        return String.format(
                "  Type              : Fixed Deposit%n" +
                "  Principal         : ₹%,.2f%n" +
                "  Interest Rate     : %.1f%% p.a.%n" +
                "  Tenure            : %d months%n" +
                "  Maturity Date     : %s%n" +
                "  Days to Maturity  : %d%n" +
                "  Projected Payout  : ₹%,.2f%n" +
                "  Status            : %s",
                principal,
                AccountType.FIXED_DEPOSIT.getInterestRate(),
                tenureMonths,
                maturityDate,
                daysToMaturity(),
                projectedMaturityAmount(),
                matured ? "MATURED" : prematurelyClosed ? "CLOSED EARLY" : "ACTIVE");
    }
}
