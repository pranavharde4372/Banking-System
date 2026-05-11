package com.bank.strategy;

import com.bank.model.Account;

/**
 * Strategy interface for interest calculation.
 *
 * <p>Applying the <strong>Strategy</strong> design pattern decouples the
 * interest formula from the account model, allowing new rate schemes to be
 * added without modifying existing classes.</p>
 */
public interface InterestStrategy {

    /**
     * Computes the interest to credit for the given account over {@code days}.
     *
     * @param account the account to calculate interest for
     * @param days    number of days in the interest period
     * @return calculated interest amount (≥ 0)
     */
    double calculateInterest(Account account, int days);

    /** Human-readable name of this strategy (used in reports). */
    String getStrategyName();
}
