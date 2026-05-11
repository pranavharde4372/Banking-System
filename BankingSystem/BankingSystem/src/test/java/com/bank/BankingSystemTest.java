package com.bank;

import com.bank.exception.*;
import com.bank.model.*;
import com.bank.service.BankingService;
import com.bank.model.Loan.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Self-contained unit test suite that does NOT require JUnit or any external
 * library.  Each test method returns {@code true} on pass, {@code false} on fail.
 *
 * <p>Run directly from the command line; the main method prints a pass/fail
 * summary and exits with code 0 (all pass) or 1 (any failure).</p>
 *
 * @author BankingSystem
 * @version 1.0
 */
public class BankingSystemTest {

    // ── Test runner infrastructure ─────────────────────────────────────────────
    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> failures = new ArrayList<>();

    private static void test(String name, boolean result) {
        if (result) {
            passed++;
            System.out.printf("  ✅ PASS  %s%n", name);
        } else {
            failed++;
            failures.add(name);
            System.out.printf("  ❌ FAIL  %s%n", name);
        }
    }

    private static void section(String title) {
        System.out.printf("%n── %s ──────────────────────────────%n", title);
    }

    // ── Helper: fresh service per test group ───────────────────────────────────
    // BankingService is a singleton, so we use unique customer/account IDs.
    private static final BankingService bank = BankingService.getInstance();
    private static final AtomicInteger  uid  = new AtomicInteger(900);

    private static String newCid() { return "T-CUST-" + uid.incrementAndGet(); }

    private static Customer newCustomer(String cid) {
        return bank.registerCustomer(new Customer.Builder(
                cid, "Test", "User-" + cid, LocalDate.of(1990, 1, 1)).build());
    }

    // =========================================================================
    // TEST METHODS
    // =========================================================================

    // ── Customer Tests ─────────────────────────────────────────────────────────
    static void testCustomerRegistration() {
        section("Customer Registration");

        String cid = newCid();
        Customer c = newCustomer(cid);
        test("Customer is created with correct ID",    c.getCustomerId().equals(cid));
        test("Customer full name is set",              c.getFullName().contains("Test"));
        test("Customer is active by default",          c.isActive());
        test("Customer has zero accounts initially",   c.getAccountNumbers().isEmpty());

        // Duplicate registration
        boolean threwDuplicate = false;
        try { bank.registerCustomer(new Customer.Builder(
                cid, "Dup", "User", LocalDate.of(1990,1,1)).build());
        } catch (BankingException e) { threwDuplicate = true; }
        test("Duplicate customer ID throws exception", threwDuplicate);
    }

    // ── Account Creation Tests ─────────────────────────────────────────────────
    static void testAccountCreation() {
        section("Account Creation");

        String cid = newCid();
        newCustomer(cid);

        Account sa = bank.openSavingsAccount(cid, 5_000);
        test("Savings account created with correct balance", sa.getBalance() == 5_000);
        test("Savings account type is SAVINGS", sa.getAccountType() == AccountType.SAVINGS);
        test("Account is active on creation",  sa.isActive());

        Account ca = bank.openCurrentAccount(cid, 10_000);
        test("Current account balance correct", ca.getBalance() == 10_000);

        // Below minimum balance
        boolean threwMin = false;
        try { bank.openSavingsAccount(cid, 100); }
        catch (InvalidTransactionException e) { threwMin = true; }
        test("Below-minimum deposit throws exception", threwMin);
    }

    // ── Deposit Tests ──────────────────────────────────────────────────────────
    static void testDeposits() {
        section("Deposits");

        String  cid     = newCid();
        newCustomer(cid);
        Account account = bank.openSavingsAccount(cid, 5_000);
        double  before  = account.getBalance();

        Transaction tx = bank.deposit(account.getAccountNumber(), 3_000, "Test deposit");
        test("Balance increases after deposit", account.getBalance() == before + 3_000);
        test("Transaction type is DEPOSIT",     tx.getType() == TransactionType.DEPOSIT);
        test("Transaction amount is correct",   tx.getAmount() == 3_000);
        test("Balance after matches account",   tx.getBalanceAfter() == account.getBalance());
        test("Transaction history grows",       account.getTransactionCount() == 1);

        // Zero deposit
        boolean threwZero = false;
        try { bank.deposit(account.getAccountNumber(), 0, "Zero"); }
        catch (InvalidTransactionException e) { threwZero = true; }
        test("Zero deposit throws exception", threwZero);

        // Negative deposit
        boolean threwNeg = false;
        try { bank.deposit(account.getAccountNumber(), -500, "Negative"); }
        catch (InvalidTransactionException e) { threwNeg = true; }
        test("Negative deposit throws exception", threwNeg);
    }

    // ── Withdrawal Tests ───────────────────────────────────────────────────────
    static void testWithdrawals() {
        section("Withdrawals");

        String  cid     = newCid();
        newCustomer(cid);
        Account account = bank.openSavingsAccount(cid, 10_000);

        Transaction tx = bank.withdraw(account.getAccountNumber(), 2_000, "ATM");
        test("Balance decreases after withdrawal",   account.getBalance() == 8_000);
        test("Transaction type is WITHDRAWAL",       tx.getType() == TransactionType.WITHDRAWAL);

        // Below minimum balance
        boolean threwMin = false;
        try { bank.withdraw(account.getAccountNumber(), 8_000, "Drain"); }
        catch (InsufficientFundsException e) { threwMin = true; }
        test("Withdrawal below minimum throws InsufficientFunds", threwMin);
    }

    // ── Transfer Tests ─────────────────────────────────────────────────────────
    static void testTransfers() {
        section("Fund Transfers");

        String cidA = newCid(), cidB = newCid();
        newCustomer(cidA); newCustomer(cidB);
        Account from = bank.openCurrentAccount(cidA, 20_000);
        Account to   = bank.openSavingsAccount(cidB,  5_000);

        double fromBefore = from.getBalance();
        double toBefore   = to.getBalance();

        bank.transfer(from.getAccountNumber(), to.getAccountNumber(), 5_000, "Test");

        test("Source balance decreases",      from.getBalance() == fromBefore - 5_000);
        test("Target balance increases",      to.getBalance()   == toBefore   + 5_000);
        test("Source tx history has entry",   from.getTransactionCount() >= 1);
        test("Target tx history has entry",   to.getTransactionCount()   >= 1);

        // Self-transfer
        boolean threwSelf = false;
        try { bank.transfer(from.getAccountNumber(), from.getAccountNumber(), 100, "Self"); }
        catch (InvalidTransactionException e) { threwSelf = true; }
        test("Self-transfer throws exception", threwSelf);

        // Insufficient funds
        boolean threwInsuf = false;
        try { bank.transfer(from.getAccountNumber(), to.getAccountNumber(), 1_000_000, "Mega"); }
        catch (InsufficientFundsException e) { threwInsuf = true; }
        test("Insufficient funds transfer throws", threwInsuf);
    }

    // ── Interest Tests ─────────────────────────────────────────────────────────
    static void testInterestCalculation() {
        section("Interest Calculation");

        String cid = newCid();
        newCustomer(cid);
        Account sa = bank.openSavingsAccount(cid, 36_500);  // easy maths

        double before = sa.getBalance();
        bank.applyInterestToAll(365);  // 1 year

        double expectedInterest = 36_500 * 0.04;
        double actualInterest   = sa.getBalance() - before;
        test("Savings interest ~4% p.a.",
                Math.abs(actualInterest - expectedInterest) < 1.0);
        test("Interest earned tracked",
                sa.getInterestEarned() > 0);
    }

    // ── Loan Tests ─────────────────────────────────────────────────────────────
    static void testLoans() {
        section("Loan Management");

        String cid = newCid();
        newCustomer(cid);
        Account account = bank.openSavingsAccount(cid, 5_000);

        Loan loan = bank.applyForLoan(
                account.getAccountNumber(), LoanType.PERSONAL, 12_000, 12.0, 12);

        test("Loan is active",                     loan.getStatus() == LoanStatus.ACTIVE);
        test("Loan amount credited to account",    account.getBalance() > 5_000);
        test("EMI is positive",                    loan.getMonthlyEMI() > 0);
        test("Total payable > principal",          loan.getTotalAmountPayable() > 12_000);

        // Make one EMI payment
        LoanPayment payment = bank.repayLoan(loan.getLoanId(), loan.getMonthlyEMI());
        test("Payment recorded",                   payment.getEmiNumber() == 1);
        test("Principal component > 0",            payment.getPrincipalComponent() > 0);
        test("Interest component > 0",             payment.getInterestComponent() > 0);
        test("EMI paid count increments",          loan.getEmiPaid() == 1);
        test("Outstanding decreases after payment",loan.getOutstandingPrincipal() < 12_000);
    }

    // ── Fixed Deposit Tests ────────────────────────────────────────────────────
    static void testFixedDeposit() {
        section("Fixed Deposit");

        String cid = newCid();
        newCustomer(cid);
        FixedDepositAccount fd = (FixedDepositAccount)
                bank.openFixedDeposit(cid, 50_000, 12);

        test("FD type is FIXED_DEPOSIT",    fd.getAccountType() == AccountType.FIXED_DEPOSIT);
        test("FD principal is correct",     fd.getPrincipal() == 50_000);
        test("FD not matured on creation",  !fd.isMatured());
        test("Days to maturity > 0",        fd.daysToMaturity() > 0);
        test("Projected amount > principal",fd.projectedMaturityAmount() > 50_000);

        // Premature withdrawal should throw before maturity
        boolean threwPremature = false;
        try { bank.withdraw(fd.getAccountNumber(), 1_000, "Premature"); }
        catch (InvalidTransactionException e) { threwPremature = true; }
        test("Premature FD withdrawal blocked", threwPremature);
    }

    // ── Concurrency Test ───────────────────────────────────────────────────────
    static void testConcurrency() throws InterruptedException {
        section("Thread Safety (Concurrent Deposits)");

        String cid = newCid();
        newCustomer(cid);
        Account account = bank.openSavingsAccount(cid, 1_000);

        int threads  = 10;
        int opsEach  = 50;
        double each  = 100.0;

        ExecutorService exec = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads * opsEach);

        for (int i = 0; i < threads * opsEach; i++) {
            exec.submit(() -> {
                try { bank.deposit(account.getAccountNumber(), each, "Concurrent"); }
                finally { latch.countDown(); }
            });
        }
        latch.await(10, TimeUnit.SECONDS);
        exec.shutdown();

        double expected = 1_000 + threads * opsEach * each;
        test("Balance correct after " + (threads * opsEach) + " concurrent deposits",
                account.getBalance() == expected);
    }

    // ── Optional / Stream Tests ────────────────────────────────────────────────
    static void testJava8Features() {
        section("Java 8 Features");

        // Optional: present
        String cid = newCid();
        newCustomer(cid);
        Account acct = bank.openSavingsAccount(cid, 5_000);
        test("Optional.isPresent for valid account",
                bank.findAccount(acct.getAccountNumber()).isPresent());

        // Optional: empty
        test("Optional.isEmpty for missing account",
                bank.findAccount("INVALID-ACC").isEmpty());

        // Stream: high value accounts
        bank.deposit(acct.getAccountNumber(), 200_000, "Big deposit");
        List<Account> highVal = bank.getHighValueAccounts(100_000);
        test("High value stream returns accounts above threshold",
                highVal.stream().allMatch(a -> a.getBalance() >= 100_000));

        // Stream: total deposits > 0
        test("getTotalDeposits > 0",
                bank.getTotalDeposits() > 0);

        // Stream: group by type has all 3 types
        Map<AccountType, List<Account>> byType = bank.getAccountsByType();
        test("getAccountsByType returns SAVINGS bucket",
                byType.containsKey(AccountType.SAVINGS));
    }

    // =========================================================================
    // MAIN – run all tests
    // =========================================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║     🧪  BANKING SYSTEM – UNIT TEST SUITE        ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        testCustomerRegistration();
        testAccountCreation();
        testDeposits();
        testWithdrawals();
        testTransfers();
        testInterestCalculation();
        testLoans();
        testFixedDeposit();
        testConcurrency();
        testJava8Features();

        System.out.println("\n" + "═".repeat(50));
        System.out.printf("  Results: %d passed, %d failed%n", passed, failed);
        if (!failures.isEmpty()) {
            System.out.println("  Failed tests:");
            failures.forEach(f -> System.out.println("    ❌ " + f));
        }
        System.out.println("═".repeat(50));

        System.exit(failed == 0 ? 0 : 1);
    }
}
