package com.bank.util;

import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.model.Loan;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Handles file-based persistence for accounts, customers, and loans.
 *
 * <p>Uses Java's built-in object serialization to write the in-memory maps
 * to disk and reload them on startup.  A timestamped backup is created
 * before each save so that data can be recovered if a write fails.</p>
 */
public final class FileManager {

    private static final FileManager INSTANCE = new FileManager();
    private FileManager() {
        try {
            Files.createDirectories(Paths.get("data"));
            Files.createDirectories(Paths.get("data/backups"));
        } catch (IOException ignored) {}
    }

    public static FileManager getInstance() { return INSTANCE; }

    private static final String ACCOUNTS_FILE  = "data/accounts.dat";
    private static final String CUSTOMERS_FILE = "data/customers.dat";
    private static final String LOANS_FILE     = "data/loans.dat";

    private final BankLogger logger = BankLogger.getInstance();

    // ── Accounts ───────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Map<String, Account> loadAccounts() {
        return (Map<String, Account>) loadObject(ACCOUNTS_FILE)
                .orElse(new HashMap<String, Account>());
    }

    public boolean saveAccounts(Map<String, Account> accounts) {
        return saveObject(accounts, ACCOUNTS_FILE);
    }

    // ── Customers ──────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Map<String, Customer> loadCustomers() {
        return (Map<String, Customer>) loadObject(CUSTOMERS_FILE)
                .orElse(new HashMap<String, Customer>());
    }

    public boolean saveCustomers(Map<String, Customer> customers) {
        return saveObject(customers, CUSTOMERS_FILE);
    }

    // ── Loans ──────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Map<String, Loan> loadLoans() {
        return (Map<String, Loan>) loadObject(LOANS_FILE)
                .orElse(new HashMap<String, Loan>());
    }

    public boolean saveLoans(Map<String, Loan> loans) {
        return saveObject(loans, LOANS_FILE);
    }

    // ── Generic helpers ────────────────────────────────────────────────────────
    private Optional<Object> loadObject(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return Optional.empty();

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(filePath)))) {
            return Optional.of(ois.readObject());
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Failed to load from " + filePath, e);
            return Optional.empty();
        }
    }

    private boolean saveObject(Object obj, String filePath) {
        // Backup existing file first
        backupFile(filePath);

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(filePath)))) {
            oos.writeObject(obj);
            logger.info("Data saved to " + filePath);
            return true;
        } catch (IOException e) {
            logger.error("Failed to save to " + filePath, e);
            return false;
        }
    }

    private void backupFile(String filePath) {
        Path src = Paths.get(filePath);
        if (!Files.exists(src)) return;

        String ts  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = Paths.get(filePath).getFileName().toString();
        Path   dst  = Paths.get("data/backups/" + fileName + "." + ts + ".bak");

        try { Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException e) { logger.warn("Backup failed for " + filePath); }
    }

    /** Exports a plain-text account statement to the data folder. */
    public boolean exportStatement(String accountNumber, List<String> lines) {
        String fileName = "data/" + accountNumber + "_statement.txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
            lines.forEach(pw::println);
            return true;
        } catch (IOException e) {
            logger.error("Statement export failed", e);
            return false;
        }
    }
}
