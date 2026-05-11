package com.bank.strategy;

import com.bank.model.Account;
import com.bank.model.AccountType;

/**
 * Simple interest on daily balance for Savings accounts (4% p.a.).
 */
public class SavingsInterestStrategy implements InterestStrategy {

    @Override
    public double calculateInterest(Account account, int days) {
        return account.getBalance()
                * AccountType.SAVINGS.getInterestRate() / 100.0
                * days / 365.0;
    }

    @Override
    public String getStrategyName() { return "Savings Simple Interest (4% p.a.)"; }
}
