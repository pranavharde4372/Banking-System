package com.bank.strategy;

import com.bank.model.Account;
import com.bank.model.AccountType;
import com.bank.model.FixedDepositAccount;

/**
 * Compound-interest calculation for Fixed Deposits (6.5% p.a., quarterly compounding).
 */
public class FixedDepositInterestStrategy implements InterestStrategy {

    private static final double RATE = AccountType.FIXED_DEPOSIT.getInterestRate() / 100.0;
    private static final int    COMPOUNDS_PER_YEAR = 4; // quarterly

    @Override
    public double calculateInterest(Account account, int days) {
        if (!(account instanceof FixedDepositAccount)) return 0.0;
        FixedDepositAccount fd = (FixedDepositAccount) account;
        if (fd.isMatured() || fd.isPrematurelyClosed()) return 0.0;

        double principal  = fd.getPrincipal();
        double years      = (double) days / 365.0;
        // A = P(1 + r/n)^(nt)
        double maturityAmount = principal
                * Math.pow(1 + RATE / COMPOUNDS_PER_YEAR,
                           COMPOUNDS_PER_YEAR * years);
        return maturityAmount - principal - account.getInterestEarned();
    }

    @Override
    public String getStrategyName() {
        return "Fixed Deposit Compound Interest (6.5% p.a., quarterly)";
    }
}
