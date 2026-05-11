package com.bank.exception;

/**
 * Base exception for all banking operations.
 * Provides structured error codes for categorizing failures.
 *
 * @author BankingSystem
 * @version 1.0
 */
public class BankingException extends RuntimeException {

    private final String errorCode;
    private final String accountNumber;

    public BankingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.accountNumber = null;
    }

    public BankingException(String message, String errorCode, String accountNumber) {
        super(message);
        this.errorCode = errorCode;
        this.accountNumber = accountNumber;
    }

    public BankingException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.accountNumber = null;
    }

    public String getErrorCode()    { return errorCode; }
    public String getAccountNumber(){ return accountNumber; }

    @Override
    public String toString() {
        return String.format("[%s] %s%s", errorCode, getMessage(),
                accountNumber != null ? " (Account: " + accountNumber + ")" : "");
    }
}
