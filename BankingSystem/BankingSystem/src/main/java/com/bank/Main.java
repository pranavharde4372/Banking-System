package com.bank;

import com.bank.exception.*;
import com.bank.model.*;
import com.bank.report.ReportService;
import com.bank.service.BankingService;
import com.bank.util.PerformanceMonitor;

import java.time.LocalDate;

/**
 * Application entry point.
 *
 * <p>Demonstrates all major features of the banking system:
 * <ul>
 *   <li>Customer registration</li>
 *   <li>Account creation (Savings, Current, Fixed Deposit)</li>
 *   <li>Deposits, withdrawals, transfers</li>
 *   <li>Interest calculation</li>
 *   <li>Loan application and repayment</li>
 *   <li>Exception handling</li>
 *   <li>Reports and analytics</li>
 *   <li>Data persistence</li>
 * </ul>
 *
 * @author BankingSystem
 * @version 1.0
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║       🏦  JAVA BANKING SYSTEM SIMULATION v1.0       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        BankingService bank   = BankingService.getInstance();
        ReportService  report = new ReportService();

        // ─────────────────────────────────────────────────────────────────────
        // STEP 1 – Register Customers
        // ─────────────────────────────────────────────────────────────────────
        section("1. CUSTOMER REGISTRATION");

        Customer priya = bank.registerCustomer(new Customer.Builder(
                "CUST-001", "Priya", "Sharma", LocalDate.of(1990, 5, 14))
                .email("priya@example.com")
                .phone("+91-9876543210")
                .address("Mumbai, Maharashtra")
                .build());

        Customer rahul = bank.registerCustomer(new Customer.Builder(
                "CUST-002", "Rahul", "Verma", LocalDate.of(1985, 11, 23))
                .email("rahul@example.com")
                .phone("+91-9123456789")
                .address("Pune, Maharashtra")
                .build());

        Customer neha = bank.registerCustomer(new Customer.Builder(
                "CUST-003", "Neha", "Singh", LocalDate.of(1995, 3, 8))
                .email("neha@example.com")
                .build());

        System.out.println("✅ Registered: " + priya);
        System.out.println("✅ Registered: " + rahul);
        System.out.println("✅ Registered: " + neha);

        // ─────────────────────────────────────────────────────────────────────
        // STEP 2 – Open Accounts
        // ─────────────────────────────────────────────────────────────────────
        section("2. ACCOUNT CREATION");

        Account priyaSavings  = bank.openSavingsAccount("CUST-001", 25_000.0);
        Account priyaCurrent  = bank.openCurrentAccount("CUST-001", 50_000.0);
        Account priyaFD       = bank.openFixedDeposit("CUST-001",  1_00_000.0, 12);
        Account rahulSavings  = bank.openSavingsAccount("CUST-002", 15_000.0);
        Account rahulCurrent  = bank.openCurrentAccount("CUST-002", 30_000.0);
        Account nehaSavings   = bank.openSavingsAccount("CUST-003", 5_000.0);

        System.out.println("✅ " + priyaSavings);
        System.out.println("✅ " + priyaCurrent);
        System.out.println("✅ " + priyaFD);
        System.out.println("✅ " + rahulSavings);
        System.out.println("✅ " + rahulCurrent);
        System.out.println("✅ " + nehaSavings);

        // ─────────────────────────────────────────────────────────────────────
        // STEP 3 – Deposits
        // ─────────────────────────────────────────────────────────────────────
        section("3. DEPOSITS");

        Transaction t1 = bank.deposit(priyaSavings.getAccountNumber(), 10_000, "Salary credit");
        Transaction t2 = bank.deposit(rahulSavings.getAccountNumber(), 5_000,  "Freelance payment");
        Transaction t3 = bank.deposit(nehaSavings.getAccountNumber(),  2_500,  "Gift from family");

        System.out.println("✅ " + t1);
        System.out.println("✅ " + t2);
        System.out.println("✅ " + t3);

        // ─────────────────────────────────────────────────────────────────────
        // STEP 4 – Withdrawals
        // ─────────────────────────────────────────────────────────────────────
        section("4. WITHDRAWALS");

        Transaction t4 = bank.withdraw(priyaSavings.getAccountNumber(), 3_000, "Grocery shopping");
        Transaction t5 = bank.withdraw(rahulCurrent.getAccountNumber(), 7_500, "Rent payment");
        System.out.println("✅ " + t4);
        System.out.println("✅ " + t5);

        // ─────────────────────────────────────────────────────────────────────
        // STEP 5 – Transfers
        // ─────────────────────────────────────────────────────────────────────
        section("5. FUND TRANSFERS");

        Transaction t6 = bank.transfer(
                priyaCurrent.getAccountNumber(),
                rahulSavings.getAccountNumber(),
                12_000, "Loan repayment to Rahul");
        System.out.println("✅ Transfer: " + t6);

        Transaction t7 = bank.transfer(
                rahulSavings.getAccountNumber(),
                nehaSavings.getAccountNumber(),
                2_000, "Shared expenses");
        System.out.println("✅ Transfer: " + t7);

        // ─────────────────────────────────────────────────────────────────────
        // STEP 6 – Interest Calculation
        // ─────────────────────────────────────────────────────────────────────
        section("6. INTEREST CALCULATIONS (30 days)");

        double interest = bank.applyInterestToAll(30);
        System.out.printf("✅ Total interest applied: ₹%,.2f%n", interest);

        // ─────────────────────────────────────────────────────────────────────
        // STEP 7 – Loans
        // ─────────────────────────────────────────────────────────────────────
        section("7. LOAN MANAGEMENT");

        Loan personalLoan = bank.applyForLoan(
                rahulSavings.getAccountNumber(),
                Loan.LoanType.PERSONAL,
                50_000.0, 10.5, 24);
        System.out.println("✅ Loan approved: " + personalLoan);
        System.out.printf("   Monthly EMI  : ₹%,.2f%n", personalLoan.getMonthlyEMI());
        System.out.printf("   Total payable: ₹%,.2f%n", personalLoan.getTotalAmountPayable());
        System.out.printf("   Total interest: ₹%,.2f%n", personalLoan.getTotalInterestPayable());

        // Pay 3 EMIs
        System.out.println("\n  EMI Payments:");
        for (int i = 1; i <= 3; i++) {
            Loan.LoanPayment payment = bank.repayLoan(
                    personalLoan.getLoanId(), personalLoan.getMonthlyEMI());
            System.out.println("  ✅ " + payment);
        }

        // ─────────────────────────────────────────────────────────────────────
        // STEP 8 – Exception Handling Demo
        // ─────────────────────────────────────────────────────────────────────
        section("8. EXCEPTION HANDLING");

        // Insufficient funds
        tryOperation("Overdraw Savings", () ->
                bank.withdraw(nehaSavings.getAccountNumber(), 9_000, "Attempt overdraw"));

        // Account not found
        tryOperation("Access non-existent account", () ->
                bank.findAccount("ACC-9999").orElseThrow(() ->
                        new AccountNotFoundException("ACC-9999")));

        // Invalid amount
        tryOperation("Zero deposit", () ->
                bank.deposit(priyaSavings.getAccountNumber(), 0, "Bad deposit"));

        // Same-account transfer
        tryOperation("Self-transfer", () ->
                bank.transfer(priyaSavings.getAccountNumber(),
                        priyaSavings.getAccountNumber(), 100, "Self"));

        // ─────────────────────────────────────────────────────────────────────
        // STEP 9 – Account Statements
        // ─────────────────────────────────────────────────────────────────────
        section("9. ACCOUNT STATEMENT");
        bank.printStatement(priyaSavings.getAccountNumber());

        // ─────────────────────────────────────────────────────────────────────
        // STEP 10 – Analytical Reports
        // ─────────────────────────────────────────────────────────────────────
        section("10. ANALYTICS & REPORTS");
        bank.printSummaryReport();
        report.printTransactionReport();
        report.printCustomerPortfolioReport();
        report.printLoanReport();
        report.printInterestProjection();

        // ─────────────────────────────────────────────────────────────────────
        // STEP 11 – Java 8 Stream Demos
        // ─────────────────────────────────────────────────────────────────────
        section("11. JAVA 8 FEATURES SHOWCASE");

        System.out.println("  High-value accounts (>₹20,000):");
        bank.getHighValueAccounts(20_000).forEach(a ->
                System.out.printf("    %-10s %-20s ₹%,.2f%n",
                        a.getAccountNumber(), a.getAccountType().getDisplayName(), a.getBalance()));

        System.out.println("\n  Balance statistics:");
        var stats = bank.getBalanceStatistics();
        System.out.printf("    Count: %d | Min: ₹%,.2f | Max: ₹%,.2f | Avg: ₹%,.2f%n",
                stats.getCount(), stats.getMin(), stats.getMax(), stats.getAverage());

        System.out.println("\n  Top 5 transactions:");
        bank.getTopTransactions(5).forEach(tx ->
                System.out.printf("    %-8s ₹%,.2f  %s%n",
                        tx.getType().getDisplayName(), tx.getAmount(), tx.getDescription()));

        System.out.printf("%n  findAccount using Optional: %s%n",
                bank.findAccount(priyaFD.getAccountNumber())
                        .map(a -> "Found – " + a.getAccountType().getDisplayName())
                        .orElse("Not found"));

        // ─────────────────────────────────────────────────────────────────────
        // STEP 12 – Persist Data
        // ─────────────────────────────────────────────────────────────────────
        section("12. DATA PERSISTENCE");
        boolean saved = bank.saveAll();
        System.out.println(saved ? "✅ Data persisted to disk" : "❌ Save failed");

        // ─────────────────────────────────────────────────────────────────────
        // STEP 13 – Performance Metrics
        // ─────────────────────────────────────────────────────────────────────
        PerformanceMonitor.getInstance().printReport();

        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║          ✅  SIMULATION COMPLETE                    ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static void section(String title) {
        System.out.println("\n┌─ " + title + " " + "─".repeat(Math.max(0, 50 - title.length())));
    }

    /** Wraps an operation in a try-catch and prints the result. */
    private static void tryOperation(String label, Runnable op) {
        try {
            op.run();
            System.out.println("  [" + label + "] Succeeded (unexpected)");
        } catch (BankingException e) {
            System.out.printf("  ✅ [%s] Caught %s: %s%n",
                    label, e.getClass().getSimpleName(), e.getMessage());
        } catch (Exception e) {
            System.out.printf("  ✅ [%s] Caught %s: %s%n",
                    label, e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
