package com.bank.exception;

/** Thrown when an account cannot be located in the system. */
public class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber, "ACCOUNT_NOT_FOUND", accountNumber);
    }
}
