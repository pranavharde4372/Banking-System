package com.bank.exception;

/** Thrown when a loan operation violates business rules. */
public class LoanException extends BankingException {
    public LoanException(String message) {
        super(message, "LOAN_ERROR");
    }
    public LoanException(String message, String accountNumber) {
        super(message, "LOAN_ERROR", accountNumber);
    }
}
