package com.bank.service;

import com.bank.exception.*;
import com.bank.factory.AccountFactory;
import com.bank.model.*;
import com.bank.observer.*;
import com.bank.strategy.*;
import com.bank.util.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Central banking service – the façade over all banking operations.
 *
 * <p>This class is the main entry point for all client code.  It:
 * <ul>
 *   <li>Maintains in-memory maps for accounts, customers, and loans.</li>
 *   <li>Dispatches {@link BankingEvent}s to registered observers.</li>
 *   <li>Delegates account creation to {@link AccountFactory}.</li>
 *   <li>Delegates interest calculation to the correct {@link InterestStrategy}.</li>
 *   <li>Wraps all operations with performance monitoring.</li>
 * </ul>
 *
 * <p><strong>Design patterns used:</strong>
 * <ul>
 *   <li>Singleton – single service instance per JVM.</li>
 *   <li>Factory   – account creation via {@link AccountFactory}.</li>
 *   <li>Strategy  – interest via {@link InterestStrategy} implementations.</li>
 *   <li>Observer  – event notification via {@link TransactionObserver}.</li>
 *   <li>Builder   – {@link Transaction} and {@link Customer} creation.</li>
 * </ul>
 *
 * @author BankingSystem
 * @version 1.0
 */
public class BankingService {

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static final BankingService INSTANCE = new BankingService();
    private BankingService() { initialize(); }
    public static BankingService getInstance() { return INSTANCE; }

    // ── Infrastructure ─────────────────────────────────────────────────────────
    private final BankLogger         logger  = BankLogger.getInstance();
    private final PerformanceMonitor monitor = PerformanceMonitor.getInstance();
    private final AccountFactory     factory = AccountFactory.getInstance();
    private final FileManager        fileManager = FileManager.getInstance();

    // ── State ──────────────────────────────────────────────────────────────────
    private Map<String, Account>  accounts;
    private Map<String, Customer> customers;
    private Map<String, Loan>     loans;

    // ── Observers ──────────────────────────────────────────────────────────────
    private final List<TransactionObserver> observers = new ArrayList<>();

    // ── Loan ID counter ────────────────────────────────────────────────────────
    private final AtomicInteger loanCounter = new AtomicInteger(100);

    // ── Interest strategies ────────────────────────────────────────────────────
    private final Map<AccountType, InterestStrategy> strategies = Map.of(
            AccountType.SAVINGS,       new SavingsInterestStrategy(),
            AccountType.CURRENT,       new CurrentInterestStrategy(),
            AccountType.FIXED_DEPOSIT, new FixedDepositInterestStrategy()
    );

    // ── Constants ──────────────────────────────────────────────────────────────
    private static final double LARGE_TX_THRESHOLD = 100_000.0;
    private static final double LOW_BALANCE_RATIO  = 1.2;   // 120% of minimum

    // ── Initialization ─────────────────────────────────────────────────────────
    private void initialize() {
        // Try to load persisted state; fall back to empty maps
        accounts  = fileManager.loadAccounts();
        customers = fileManager.loadCustomers();
        loans     = fileManager.loadLoans();

        // Register default observers
        registerObserver(new AuditObserver());
        registerObserver(new FraudDetectionObserver());

        logger.info("BankingService initialized. Accounts=" + accounts.size()
                + ", Customers=" + customers.size()
                + ", Loans=" + loans.size());
    }

    // =========================================================================
    // OBSERVER MANAGEMENT
    // =========================================================================

    public void registerObserver(TransactionObserver observer) {
        observers.add(Objects.requireNonNull(observer));
    }

    public void removeObserver(TransactionObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(BankingEvent event) {
        observers.forEach(o -> {
            try { o.onEvent(event); }
            catch (Exception e) { logger.warn("Observer error: " + e.getMessage()); }
        });
    }

    // =========================================================================
    // CUSTOMER MANAGEMENT
    // =========================================================================

    /**
     * Registers a new customer in the system.
     *
     * @param customer fully-built {@link Customer} object
     * @return the registered customer
     * @throws BankingException if the customer ID already exists
     */
    public Customer registerCustomer(Customer customer) {
        if (customers.containsKey(customer.getCustomerId()))
            throw new BankingException("Customer already exists: " + customer.getCustomerId(),
                    "DUPLICATE_CUSTOMER");
        customers.put(customer.getCustomerId(), customer);
        logger.info("Customer registered: " + customer.getCustomerId() + " – " + customer.getFullName());
        return customer;
    }

    public Optional<Customer> findCustomer(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }

    // =========================================================================
    // ACCOUNT MANAGEMENT
    // =========================================================================

    /**
     * Opens a Savings account for an existing customer.
     *
     * @param customerId     existing customer
     * @param initialDeposit opening amount (≥ ₹500)
     * @return the new {@link SavingsAccount}
     */
    public Account openSavingsAccount(String customerId, double initialDeposit) {
        return openAccount(AccountType.SAVINGS, customerId, initialDeposit);
    }

    /** Opens a Current (chequing) account. */
    public Account openCurrentAccount(String customerId, double initialDeposit) {
        return openAccount(AccountType.CURRENT, customerId, initialDeposit);
    }

    /** Opens a Fixed Deposit account with the given tenure. */
    public Account openFixedDeposit(String customerId, double principal, int tenureMonths) {
        long start = System.nanoTime();
        Customer customer = getCustomerOrThrow(customerId);
        FixedDepositAccount fd = factory.createFixedDeposit(customerId, principal, tenureMonths);
        accounts.put(fd.getAccountNumber(), fd);
        customer.linkAccount(fd.getAccountNumber());
        monitor.record("openFixedDeposit", System.nanoTime() - start);
        logger.info("FD opened: " + fd.getAccountNumber() + " Principal=₹" + principal);
        return fd;
    }

    private Account openAccount(AccountType type, String customerId, double initialDeposit) {
        long start = System.nanoTime();
        Customer customer = getCustomerOrThrow(customerId);
        Account account   = factory.createAccount(type, customerId, initialDeposit);
        accounts.put(account.getAccountNumber(), account);
        customer.linkAccount(account.getAccountNumber());
        monitor.record("openAccount", System.nanoTime() - start);
        logger.info("Account opened: " + account.getAccountNumber()
                + " Type=" + type + " Balance=₹" + initialDeposit);
        return account;
    }

    /**
     * Retrieves an account by number wrapped in {@link Optional}.
     *
     * <p>Uses Java 8 {@link Optional} for null-safe lookups.</p>
     */
    public Optional<Account> findAccount(String accountNumber) {
        return Optional.ofNullable(accounts.get(accountNumber));
    }

    /** Closes an account (marks it inactive; does not delete data). */
    public void closeAccount(String accountNumber, String reason) {
        Account account = getAccountOrThrow(accountNumber);
        account.setActive(false);
        notifyObservers(new BankingEvent(
                BankingEvent.EventType.ACCOUNT_CLOSED, account, null,
                "Account closed: " + reason));
        logger.info("Account closed: " + accountNumber + " – " + reason);
    }

    /** Freezes / unfreezes an account. */
    public void setAccountFrozen(String accountNumber, boolean frozen, String reason) {
        Account account = getAccountOrThrow(accountNumber);
        account.setFrozen(frozen);
        notifyObservers(new BankingEvent(
                BankingEvent.EventType.ACCOUNT_FROZEN, account, null,
                (frozen ? "Frozen" : "Unfrozen") + ": " + reason));
        logger.info("Account " + (frozen ? "frozen" : "unfrozen") + ": " + accountNumber);
    }

    // =========================================================================
    // CORE BANKING OPERATIONS
    // =========================================================================

    /**
     * Deposits money into an account.
     *
     * @param accountNumber target account
     * @param amount        positive amount in ₹
     * @param description   purpose of deposit
     * @return the resulting {@link Transaction}
     */
    public Transaction deposit(String accountNumber, double amount, String description) {
        long start = System.nanoTime();
        Account account = getAccountOrThrow(accountNumber);
        Transaction tx  = account.deposit(amount, description);
        postTransactionEvents(account, tx);
        monitor.record("deposit", System.nanoTime() - start);
        return tx;
    }

    /**
     * Withdraws money from an account.
     *
     * @param accountNumber source account
     * @param amount        positive amount in ₹
     * @param description   purpose of withdrawal
     * @return the resulting {@link Transaction}
     */
    public Transaction withdraw(String accountNumber, double amount, String description) {
        long start = System.nanoTime();
        Account account = getAccountOrThrow(accountNumber);
        Transaction tx  = account.withdraw(amount, description);
        postTransactionEvents(account, tx);
        monitor.record("withdraw", System.nanoTime() - start);
        return tx;
    }

    /**
     * Transfers money between two accounts atomically.
     *
     * <p>Both debit and credit are executed inside a synchronized block so that
     * a partial failure does not leave the system in an inconsistent state.</p>
     *
     * @param fromAccountNumber source account
     * @param toAccountNumber   destination account
     * @param amount            transfer amount
     * @param description       transfer memo
     * @return the debit {@link Transaction} (the credit is recorded on the target)
     */
    public Transaction transfer(String fromAccountNumber, String toAccountNumber,
                                double amount, String description) {
        long start = System.nanoTime();

        if (fromAccountNumber.equals(toAccountNumber))
            throw new InvalidTransactionException("Cannot transfer to the same account");

        Account from = getAccountOrThrow(fromAccountNumber);
        Account to   = getAccountOrThrow(toAccountNumber);

        // Lock both accounts in a consistent order to prevent deadlock
        Object first  = fromAccountNumber.compareTo(toAccountNumber) < 0 ? from : to;
        Object second = first == from ? to : from;

        synchronized (first) {
            synchronized (second) {
                // Debit
                from.withdraw(amount, description + " [Transfer to " + toAccountNumber + "]");
                Transaction debitTx = new Transaction.Builder()
                        .accountNumber(fromAccountNumber)
                        .type(TransactionType.TRANSFER_OUT)
                        .amount(amount)
                        .balanceAfter(from.getBalance())
                        .description("Transfer to " + toAccountNumber + ": " + description)
                        .referenceId(toAccountNumber)
                        .build();
                from.addTransaction(debitTx);

                // Credit
                to.deposit(amount, description + " [Transfer from " + fromAccountNumber + "]");
                Transaction creditTx = new Transaction.Builder()
                        .accountNumber(toAccountNumber)
                        .type(TransactionType.TRANSFER_IN)
                        .amount(amount)
                        .balanceAfter(to.getBalance())
                        .description("Transfer from " + fromAccountNumber + ": " + description)
                        .referenceId(fromAccountNumber)
                        .build();
                to.addTransaction(creditTx);

                postTransactionEvents(from, debitTx);
                postTransactionEvents(to, creditTx);
                monitor.record("transfer", System.nanoTime() - start);
                return debitTx;
            }
        }
    }

    // =========================================================================
    // INTEREST OPERATIONS
    // =========================================================================

    /**
     * Applies accrued interest to all eligible accounts for the given period.
     *
     * <p>Uses Java 8 Streams to filter and process accounts efficiently.</p>
     *
     * @param days interest period in days
     * @return total interest disbursed across all accounts
     */
    public double applyInterestToAll(int days) {
        long start = System.nanoTime();

        double totalInterest = accounts.values().stream()
                .filter(Account::isActive)
                .filter(a -> !a.isFrozen())
                .filter(a -> strategies.containsKey(a.getAccountType()))
                .mapToDouble(account -> {
                    InterestStrategy strategy = strategies.get(account.getAccountType());
                    double interest = strategy.calculateInterest(account, days);
                    if (interest > 0) {
                        account.applyInterest(interest,
                                strategy.getStrategyName() + " (" + days + " days)");
                        notifyObservers(new BankingEvent(
                                BankingEvent.EventType.INTEREST_APPLIED, account, null,
                                String.format("Interest ₹%.2f applied", interest)));
                    }
                    return interest;
                })
                .sum();

        monitor.record("applyInterest", System.nanoTime() - start);
        logger.info(String.format("Interest applied: ₹%.2f across %d accounts (period: %d days)",
                totalInterest, accounts.size(), days));
        return totalInterest;
    }

    // =========================================================================
    // LOAN MANAGEMENT
    // =========================================================================

    /**
     * Applies for a loan linked to an existing account.
     *
     * @param accountNumber      applicant's account
     * @param loanType           type of loan
     * @param principal          loan amount
     * @param annualInterestRate annual rate (%)
     * @param tenureMonths       repayment period
     * @return the approved {@link Loan}
     */
    public Loan applyForLoan(String accountNumber, Loan.LoanType loanType,
                             double principal, double annualInterestRate, int tenureMonths) {
        Account account = getAccountOrThrow(accountNumber);

        // Basic eligibility: account must be active and have positive balance
        if (account.getBalance() <= 0)
            throw new LoanException("Insufficient account standing for loan approval", accountNumber);

        String loanId = "LOAN-" + loanCounter.incrementAndGet();
        Loan loan = new Loan(loanId, accountNumber, loanType,
                principal, annualInterestRate, tenureMonths);
        loans.put(loanId, loan);

        // Disburse: credit the loan amount to the linked account
        Transaction disbursal = new Transaction.Builder()
                .accountNumber(accountNumber)
                .type(TransactionType.LOAN_DISBURSEMENT)
                .amount(principal)
                .balanceAfter(account.getBalance() + principal)
                .description("Loan disbursement: " + loanId)
                .referenceId(loanId)
                .build();
        account.deposit(principal, "Loan disbursement – " + loanId);
        account.addTransaction(disbursal);

        notifyObservers(new BankingEvent(
                BankingEvent.EventType.LOAN_APPROVED, account, disbursal,
                String.format("Loan %s approved: ₹%.2f @ %.1f%% for %d months (EMI: ₹%.2f)",
                        loanId, principal, annualInterestRate, tenureMonths, loan.getMonthlyEMI())));

        logger.info("Loan approved: " + loanId + " Amount=₹" + principal);
        return loan;
    }

    /**
     * Makes an EMI payment towards a loan.
     *
     * @param loanId  loan identifier
     * @param amount  payment amount (usually the EMI amount)
     * @return a {@link Loan.LoanPayment} record
     */
    public Loan.LoanPayment repayLoan(String loanId, double amount) {
        Loan loan = Optional.ofNullable(loans.get(loanId))
                .orElseThrow(() -> new LoanException("Loan not found: " + loanId));

        Account account = getAccountOrThrow(loan.getAccountNumber());
        account.withdraw(amount, "EMI payment for loan " + loanId);

        Loan.LoanPayment payment = loan.makePayment(amount);
        logger.info("EMI paid: " + loanId + " #" + payment.getEmiNumber()
                + " Amount=₹" + amount);
        return payment;
    }

    public Optional<Loan> findLoan(String loanId) {
        return Optional.ofNullable(loans.get(loanId));
    }

    // =========================================================================
    // JAVA 8 STREAM-BASED QUERIES
    // =========================================================================

    /**
     * Returns accounts with balance above the threshold, sorted descending.
     * Demonstrates: Stream filter, sorted, Comparator, collect.
     */
    public List<Account> getHighValueAccounts(double threshold) {
        return accounts.values().stream()
                .filter(Account::isActive)
                .filter(a -> a.getBalance() >= threshold)
                .sorted(Comparator.comparingDouble(Account::getBalance).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Groups all active accounts by their type.
     * Demonstrates: Collectors.groupingBy.
     */
    public Map<AccountType, List<Account>> getAccountsByType() {
        return accounts.values().stream()
                .filter(Account::isActive)
                .collect(Collectors.groupingBy(Account::getAccountType));
    }

    /**
     * Calculates the total balance across all active accounts.
     * Demonstrates: mapToDouble, sum.
     */
    public double getTotalDeposits() {
        return accounts.values().stream()
                .filter(Account::isActive)
                .mapToDouble(Account::getBalance)
                .sum();
    }

    /**
     * Returns accounts that have LOW balances (within 120% of the minimum).
     * Demonstrates: filter with complex predicate.
     */
    public List<Account> getLowBalanceAccounts() {
        return accounts.values().stream()
                .filter(Account::isActive)
                .filter(a -> a.getBalance() < a.getAccountType().getMinimumBalance() * LOW_BALANCE_RATIO)
                .collect(Collectors.toList());
    }

    /**
     * Computes a summary statistics object for all account balances.
     * Demonstrates: DoubleSummaryStatistics.
     */
    public java.util.DoubleSummaryStatistics getBalanceStatistics() {
        return accounts.values().stream()
                .filter(Account::isActive)
                .mapToDouble(Account::getBalance)
                .summaryStatistics();
    }

    /**
     * Finds accounts for a given customer using Lambda expressions.
     */
    public List<Account> getAccountsForCustomer(String customerId) {
        return accounts.values().stream()
                .filter(a -> a.getCustomerId().equals(customerId))
                .collect(Collectors.toList());
    }

    /**
     * Returns the top N transactions across all accounts by amount.
     * Demonstrates: flatMap, sorted, limit.
     */
    public List<Transaction> getTopTransactions(int n) {
        return accounts.values().stream()
                .flatMap(a -> a.getTransactions().stream())
                .sorted(Comparator.comparingDouble(Transaction::getAmount).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * Returns the total transaction volume for a specific type.
     * Demonstrates: filter on enum, mapToDouble, sum.
     */
    public double getTotalVolumeByType(TransactionType type) {
        return accounts.values().stream()
                .flatMap(a -> a.getTransactions().stream())
                .filter(tx -> tx.getType() == type)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    // =========================================================================
    // PERSISTENCE
    // =========================================================================

    /** Persists all in-memory state to disk. */
    public boolean saveAll() {
        boolean ok = fileManager.saveAccounts(accounts)
                   & fileManager.saveCustomers(customers)
                   & fileManager.saveLoans(loans);
        if (ok) logger.info("All data saved successfully");
        return ok;
    }

    // =========================================================================
    // REPORTING & UTILITY
    // =========================================================================

    /** Prints a formatted account statement to stdout. */
    public void printStatement(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        String line = "═".repeat(70);
        System.out.println("\n" + line);
        System.out.printf(" ACCOUNT STATEMENT – %s%n", accountNumber);
        System.out.println(line);
        System.out.println(account.getAccountDetails());
        System.out.printf("%n  Current Balance: ₹%,.2f%n", account.getBalance());
        System.out.println("\n  TRANSACTION HISTORY:");
        System.out.println("  " + "─".repeat(67));
        account.getTransactions().forEach(tx -> System.out.println("  " + tx));
        System.out.println(line + "\n");
    }

    /** Generates and prints the bank-wide summary report. */
    public void printSummaryReport() {
        Map<AccountType, List<Account>> byType = getAccountsByType();
        double total = getTotalDeposits();
        java.util.DoubleSummaryStatistics stats = getBalanceStatistics();
        long totalTx = accounts.values().stream()
                .mapToLong(Account::getTransactionCount).sum();

        System.out.println("\n" + "═".repeat(55));
        System.out.println("  🏦  JAVA BANKING SYSTEM – SUMMARY REPORT");
        System.out.println("═".repeat(55));
        System.out.printf("  Total Accounts         : %,d%n", accounts.size());
        System.out.printf("  Active Customers       : %,d%n", customers.size());
        System.out.printf("  Active Loans           : %,d%n",
                loans.values().stream().filter(l -> l.getStatus() == Loan.LoanStatus.ACTIVE).count());
        System.out.printf("  Total Deposits         : ₹%,.2f%n", total);
        System.out.printf("  Total Transactions     : %,d%n", totalTx);
        System.out.println();

        System.out.println("  ACCOUNTS BY TYPE:");
        byType.forEach((type, list) ->
                System.out.printf("  %-20s : %3d accounts  ₹%,.2f total%n",
                        type.getDisplayName(), list.size(),
                        list.stream().mapToDouble(Account::getBalance).sum()));

        System.out.println();
        System.out.println("  BALANCE STATISTICS:");
        System.out.printf("  Min   : ₹%,.2f%n", stats.getMin());
        System.out.printf("  Max   : ₹%,.2f%n", stats.getMax());
        System.out.printf("  Avg   : ₹%,.2f%n", stats.getAverage());
        System.out.printf("  Total : ₹%,.2f%n", stats.getSum());

        System.out.println();
        System.out.println("  TOP 5 HIGH-VALUE ACCOUNTS:");
        getHighValueAccounts(0).stream().limit(5).forEach(a ->
                System.out.printf("  %-10s %-20s ₹%,.2f%n",
                        a.getAccountNumber(), a.getAccountType().getDisplayName(), a.getBalance()));

        System.out.println("\n  TRANSACTION VOLUME BY TYPE:");
        for (TransactionType txType : TransactionType.values()) {
            double vol = getTotalVolumeByType(txType);
            if (vol > 0)
                System.out.printf("  %-22s : ₹%,.2f%n", txType.getDisplayName(), vol);
        }

        System.out.println("\n  INTEREST RATES:");
        strategies.forEach((type, strategy) ->
                System.out.printf("  %-20s : %.1f%% p.a.%n",
                        type.getDisplayName(), type.getInterestRate()));

        System.out.println("═".repeat(55) + "\n");
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Account getAccountOrThrow(String accountNumber) {
        return Optional.ofNullable(accounts.get(accountNumber))
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private Customer getCustomerOrThrow(String customerId) {
        return Optional.ofNullable(customers.get(customerId))
                .orElseThrow(() -> new BankingException(
                        "Customer not found: " + customerId, "CUSTOMER_NOT_FOUND"));
    }

    private void postTransactionEvents(Account account, Transaction tx) {
        notifyObservers(new BankingEvent(
                BankingEvent.EventType.TRANSACTION_COMPLETED, account, tx,
                String.format("%s ₹%.2f | Balance: ₹%.2f",
                        tx.getType().getDisplayName(), tx.getAmount(), tx.getBalanceAfter())));

        // Large transaction alert
        if (tx.getAmount() >= LARGE_TX_THRESHOLD)
            notifyObservers(new BankingEvent(
                    BankingEvent.EventType.LARGE_TRANSACTION, account, tx,
                    "Large transaction: ₹" + tx.getAmount()));

        // Low balance warning
        double minBal = account.getAccountType().getMinimumBalance();
        if (account.getBalance() < minBal * LOW_BALANCE_RATIO && minBal > 0)
            notifyObservers(new BankingEvent(
                    BankingEvent.EventType.LOW_BALANCE_WARNING, account, tx,
                    String.format("Balance ₹%.2f is near minimum ₹%.2f",
                            account.getBalance(), minBal)));
    }

    // ── Accessors for testing ──────────────────────────────────────────────────
    public Map<String, Account>  getAllAccounts()  { return Collections.unmodifiableMap(accounts); }
    public Map<String, Customer> getAllCustomers() { return Collections.unmodifiableMap(customers); }
    public Map<String, Loan>     getAllLoans()     { return Collections.unmodifiableMap(loans); }
}
