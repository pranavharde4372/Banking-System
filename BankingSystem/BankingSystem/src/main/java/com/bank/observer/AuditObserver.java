package com.bank.observer;

import com.bank.util.BankLogger;

/**
 * Logs every banking event to the application log.
 */
public class AuditObserver implements TransactionObserver {

    private final BankLogger logger = BankLogger.getInstance();

    @Override
    public void onEvent(BankingEvent event) {
        String msg = String.format("AUDIT | %s | Account: %s | %s",
                event.getType(),
                event.getAccount().getAccountNumber(),
                event.getMessage());

        if (event.getType() == BankingEvent.EventType.SUSPICIOUS_ACTIVITY ||
            event.getType() == BankingEvent.EventType.LOAN_DEFAULTED) {
            logger.warn(msg);
        } else {
            logger.info(msg);
        }
    }
}
