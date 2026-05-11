package com.bank.exception;

/** Thrown when input data fails validation rules. */
public class InvalidTransactionException extends BankingException {
    public InvalidTransactionException(String message) {
        super(message, "INVALID_TRANSACTION");
    }
    public InvalidTransactionException(String message, String accountNumber) {
        super(message, "INVALID_TRANSACTION", accountNumber);
    }
}
