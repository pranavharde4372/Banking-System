package com.bank.observer;

/**
 * Observer interface (Gang-of-Four Observer pattern).
 *
 * <p>All components that want to react to banking events must implement
 * this interface and register with the {@code BankingService}.</p>
 */
public interface TransactionObserver {
    void onEvent(BankingEvent event);
}
