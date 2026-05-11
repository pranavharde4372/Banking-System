package com.bank.report;

import com.bank.model.*;
import com.bank.service.BankingService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.*;

/**
 * Generates analytical reports using Java 8 Streams and Lambdas.
 *
 * <p>All data is pulled from the {@link BankingService}.  Methods return
 * formatted strings suitable for printing or writing to a file.</p>
 */
public class ReportService {

    private final BankingService bank = BankingService.getInstance();

    // ── Monthly transaction report ─────────────────────────────────────────────
    /**
     * Groups all transactions by date and summarises daily totals.
     * Demonstrates: flatMap, Collectors.groupingBy, Collectors.summingDouble.
     */
    public void printTransactionReport() {
        System.out.println("\n📊 TRANSACTION REPORT");
        System.out.println("─".repeat(50));

        Map<TransactionType, DoubleSummaryStatistics> statsMap =
                bank.getAllAccounts().values().stream()
                .flatMap(a -> a.getTransactions().stream())
                .collect(Collectors.groupingBy(
                        Transaction::getType,
                        Collectors.summarizingDouble(Transaction::getAmount)));

        System.out.printf("  %-22s  %6s  %12s%n", "Type", "Count", "Total (₹)");
        System.out.println("  " + "─".repeat(44));
        statsMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(TransactionType::getDisplayName)))
                .forEach(e -> System.out.printf("  %-22s  %6d  %12,.2f%n",
                        e.getKey().getDisplayName(),
                        e.getValue().getCount(),
                        e.getValue().getSum()));
    }

    // ── Customer portfolio report ──────────────────────────────────────────────
    /**
     * Prints a summary for every customer showing their total wealth.
     * Demonstrates: joining, mapToDouble, flatMap on Optional.
     */
    public void printCustomerPortfolioReport() {
        System.out.println("\n👥 CUSTOMER PORTFOLIO REPORT");
        System.out.println("─".repeat(60));
        System.out.printf("  %-12s  %-20s  %5s  %14s%n",
                "ID", "Name", "Accts", "Total Balance");
        System.out.println("  " + "─".repeat(56));

        bank.getAllCustomers().values().stream()
                .filter(Customer::isActive)
                .forEach(customer -> {
                    List<Account> accts = bank.getAccountsForCustomer(customer.getCustomerId());
                    double total = accts.stream()
                            .filter(Account::isActive)
                            .mapToDouble(Account::getBalance)
                            .sum();
                    System.out.printf("  %-12s  %-20s  %5d  ₹%12,.2f%n",
                            customer.getCustomerId(),
                            customer.getFullName(),
                            accts.size(),
                            total);
                });
    }

    // ── Loan portfolio report ──────────────────────────────────────────────────
    /**
     * Summarises the loan book by type.
     * Demonstrates: Collectors.partitioningBy, Collectors.counting.
     */
    public void printLoanReport() {
        System.out.println("\n🏦 LOAN PORTFOLIO REPORT");
        System.out.println("─".repeat(60));

        Map<Loan.LoanStatus, Long> byStatus =
                bank.getAllLoans().values().stream()
                .collect(Collectors.groupingBy(Loan::getStatus, Collectors.counting()));

        Map<Loan.LoanType, DoubleSummaryStatistics> byType =
                bank.getAllLoans().values().stream()
                .collect(Collectors.groupingBy(Loan::getLoanType,
                        Collectors.summarizingDouble(Loan::getPrincipal)));

        System.out.println("  By Status:");
        byStatus.forEach((s, c) -> System.out.printf("  %-15s : %d%n", s, c));

        System.out.println("\n  By Type:");
        System.out.printf("  %-15s  %5s  %14s%n", "Type", "Count", "Total Principal");
        System.out.println("  " + "─".repeat(40));
        byType.forEach((t, st) ->
                System.out.printf("  %-15s  %5d  ₹%12,.2f%n",
                        t, (long) st.getCount(), st.getSum()));

        double totalOutstanding = bank.getAllLoans().values().stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.ACTIVE)
                .mapToDouble(Loan::getOutstandingPrincipal)
                .sum();
        System.out.printf("%n  Total Outstanding: ₹%,.2f%n", totalOutstanding);
    }

    // ── Interest projection report ─────────────────────────────────────────────
    /**
     * Projects monthly interest income across all account types.
     */
    public void printInterestProjection() {
        System.out.println("\n📈 INTEREST PROJECTION (Monthly)");
        System.out.println("─".repeat(50));

        bank.getAccountsByType().forEach((type, accts) -> {
            double totalBalance = accts.stream()
                    .filter(Account::isActive)
                    .mapToDouble(Account::getBalance)
                    .sum();
            double monthlyInterest = totalBalance * type.getInterestRate() / 100.0 / 12.0;
            System.out.printf("  %-20s : %.1f%% → ₹%,.2f/month%n",
                    type.getDisplayName(), type.getInterestRate(), monthlyInterest);
        });

        double totalMonthly = bank.getAllAccounts().values().stream()
                .filter(Account::isActive)
                .mapToDouble(a -> a.getBalance() * a.getAccountType().getInterestRate() / 100.0 / 12.0)
                .sum();
        System.out.printf("  %-20s   ₹%,.2f/month%n", "TOTAL", totalMonthly);
    }
}
