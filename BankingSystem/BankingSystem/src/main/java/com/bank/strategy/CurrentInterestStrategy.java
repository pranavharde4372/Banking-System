package com.bank.strategy;

import com.bank.model.Account;
import com.bank.model.AccountType;

/**
 * Nominal interest on Current account balance (0.5% p.a.).
 */
public class CurrentInterestStrategy implements InterestStrategy {

    @Override
    public double calculateInterest(Account account, int days) {
        return account.getBalance()
                * AccountType.CURRENT.getInterestRate() / 100.0
                * days / 365.0;
    }

    @Override
    public String getStrategyName() { return "Current Nominal Interest (0.5% p.a.)"; }
}
