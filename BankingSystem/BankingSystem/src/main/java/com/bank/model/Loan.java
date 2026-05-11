package com.bank.model;

import com.bank.exception.LoanException;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a bank loan with EMI calculations and repayment tracking.
 *
 * <p>Uses the standard reducing-balance (amortization) method to calculate
 * the monthly EMI.  Each repayment is logged as a {@link LoanPayment}.</p>
 */
public class Loan implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum LoanStatus { ACTIVE, REPAID, DEFAULTED, REJECTED }
    public enum LoanType   { PERSONAL, HOME, VEHICLE, EDUCATION }

    // ── Core fields ────────────────────────────────────────────────────────────
    private final String     loanId;
    private final String     accountNumber;
    private final LoanType   loanType;
    private final double     principal;
    private final double     annualInterestRate;
    private final int        tenureMonths;
    private final double     monthlyEMI;
    private final LocalDate  startDate;
    private final LocalDate  endDate;

    private double     outstandingPrincipal;
    private double     totalInterestPaid;
    private int        emiPaid;
    private LoanStatus status;

    private final List<LoanPayment> payments = new ArrayList<>();

    // ── Constructor ────────────────────────────────────────────────────────────
    public Loan(String loanId, String accountNumber, LoanType loanType,
                double principal, double annualInterestRate, int tenureMonths) {
        this.loanId              = loanId;
        this.accountNumber       = accountNumber;
        this.loanType            = loanType;
        this.principal           = principal;
        this.annualInterestRate  = annualInterestRate;
        this.tenureMonths        = tenureMonths;
        this.outstandingPrincipal= principal;
        this.totalInterestPaid   = 0.0;
        this.emiPaid             = 0;
        this.status              = LoanStatus.ACTIVE;
        this.startDate           = LocalDate.now();
        this.endDate             = LocalDate.now().plusMonths(tenureMonths);
        this.monthlyEMI          = calculateEMI(principal, annualInterestRate, tenureMonths);
    }

    // ── EMI calculation (standard amortization formula) ────────────────────────
    /**
     * EMI = P × r × (1+r)^n / [(1+r)^n – 1]
     * where r = monthly rate, n = total months
     */
    public static double calculateEMI(double principal, double annualRate, int months) {
        double r = annualRate / (12 * 100.0);
        if (r == 0) return principal / months;
        double pow = Math.pow(1 + r, months);
        return principal * r * pow / (pow - 1);
    }

    // ── Repayment ──────────────────────────────────────────────────────────────
    /**
     * Records an EMI payment, splits it into interest and principal components,
     * and returns a {@link LoanPayment} record.
     */
    public LoanPayment makePayment(double amount) {
        if (status != LoanStatus.ACTIVE)
            throw new LoanException("Loan " + loanId + " is not active", accountNumber);
        if (outstandingPrincipal <= 0)
            throw new LoanException("Loan already fully repaid", accountNumber);

        double monthlyRate   = annualInterestRate / (12 * 100.0);
        double interestPart  = outstandingPrincipal * monthlyRate;
        double principalPart = Math.min(amount - interestPart, outstandingPrincipal);

        if (principalPart < 0)
            throw new LoanException(
                    String.format("Payment ₹%.2f is less than interest due ₹%.2f",
                            amount, interestPart), accountNumber);

        outstandingPrincipal -= principalPart;
        totalInterestPaid    += interestPart;
        emiPaid++;

        LoanPayment payment = new LoanPayment(loanId, emiPaid, amount,
                principalPart, interestPart, outstandingPrincipal);
        payments.add(payment);

        if (outstandingPrincipal <= 0.01) status = LoanStatus.REPAID;
        return payment;
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public String        getLoanId()              { return loanId; }
    public String        getAccountNumber()       { return accountNumber; }
    public LoanType      getLoanType()            { return loanType; }
    public double        getPrincipal()           { return principal; }
    public double        getAnnualInterestRate()  { return annualInterestRate; }
    public int           getTenureMonths()        { return tenureMonths; }
    public double        getMonthlyEMI()          { return monthlyEMI; }
    public LocalDate     getStartDate()           { return startDate; }
    public LocalDate     getEndDate()             { return endDate; }
    public double        getOutstandingPrincipal(){ return outstandingPrincipal; }
    public double        getTotalInterestPaid()   { return totalInterestPaid; }
    public int           getEmiPaid()             { return emiPaid; }
    public int           getEmiRemaining()        { return tenureMonths - emiPaid; }
    public LoanStatus    getStatus()              { return status; }
    public List<LoanPayment> getPayments()        { return Collections.unmodifiableList(payments); }

    public double getTotalAmountPayable() { return monthlyEMI * tenureMonths; }
    public double getTotalInterestPayable(){ return getTotalAmountPayable() - principal; }

    @Override
    public String toString() {
        return String.format("Loan[%s | %s | ₹%,.2f @ %.1f%% | EMI: ₹%,.2f | Outstanding: ₹%,.2f | %s]",
                loanId, loanType, principal, annualInterestRate,
                monthlyEMI, outstandingPrincipal, status);
    }

    // ── Inner record ───────────────────────────────────────────────────────────
    /**
     * Immutable record of a single loan payment.
     */
    public static class LoanPayment implements Serializable {
        private final String    loanId;
        private final int       emiNumber;
        private final double    totalPaid;
        private final double    principalComponent;
        private final double    interestComponent;
        private final double    balanceAfter;
        private final LocalDate paidOn;

        public LoanPayment(String loanId, int emiNumber, double totalPaid,
                           double principalComponent, double interestComponent,
                           double balanceAfter) {
            this.loanId              = loanId;
            this.emiNumber           = emiNumber;
            this.totalPaid           = totalPaid;
            this.principalComponent  = principalComponent;
            this.interestComponent   = interestComponent;
            this.balanceAfter        = balanceAfter;
            this.paidOn              = LocalDate.now();
        }

        public String    getLoanId()             { return loanId; }
        public int       getEmiNumber()          { return emiNumber; }
        public double    getTotalPaid()          { return totalPaid; }
        public double    getPrincipalComponent() { return principalComponent; }
        public double    getInterestComponent()  { return interestComponent; }
        public double    getBalanceAfter()       { return balanceAfter; }
        public LocalDate getPaidOn()             { return paidOn; }

        @Override
        public String toString() {
            return String.format("EMI#%d | Paid: ₹%,.2f (Principal: ₹%,.2f + Interest: ₹%,.2f) | Balance: ₹%,.2f",
                    emiNumber, totalPaid, principalComponent, interestComponent, balanceAfter);
        }
    }
}
