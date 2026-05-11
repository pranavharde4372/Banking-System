package com.bank.observer;

import com.bank.util.BankLogger;

/**
 * Detects and logs potentially suspicious activity.
 *
 * <p>Flags transactions over ₹1,00,000 and rapid successive transactions.</p>
 */
public class FraudDetectionObserver implements TransactionObserver {

    private static final double LARGE_TX_THRESHOLD = 100_000.0;
    private final BankLogger logger = BankLogger.getInstance();

    @Override
    public void onEvent(BankingEvent event) {
        if (event.getTransaction() == null) return;

        double amount = event.getTransaction().getAmount();

        if (amount >= LARGE_TX_THRESHOLD) {
            logger.warn(String.format(
                    "FRAUD-ALERT | Large transaction of ₹%,.2f on account %s",
                    amount, event.getAccount().getAccountNumber()));
        }

        if (event.getType() == BankingEvent.EventType.SUSPICIOUS_ACTIVITY) {
            logger.warn("FRAUD-ALERT | " + event.getMessage());
        }
    }
}
