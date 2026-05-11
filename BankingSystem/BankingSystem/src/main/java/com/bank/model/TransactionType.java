package com.bank.model;

/**
 * All possible transaction categories in the banking system.
 */
public enum TransactionType {
    DEPOSIT("Deposit", true),
    WITHDRAWAL("Withdrawal", false),
    TRANSFER_IN("Transfer In", true),
    TRANSFER_OUT("Transfer Out", false),
    INTEREST_CREDIT("Interest Credit", true),
    LOAN_DISBURSEMENT("Loan Disbursement", true),
    LOAN_REPAYMENT("Loan Repayment", false),
    FEE("Service Fee", false),
    PENALTY("Penalty", false);

    private final String displayName;
    private final boolean isCredit;     // true = credit, false = debit

    TransactionType(String displayName, boolean isCredit) {
        this.displayName = displayName;
        this.isCredit    = isCredit;
    }

    public String  getDisplayName() { return displayName; }
    public boolean isCredit()       { return isCredit; }
    public boolean isDebit()        { return !isCredit; }
}
