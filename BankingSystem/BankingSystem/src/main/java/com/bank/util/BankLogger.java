package com.bank.util;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lightweight thread-safe logger – Singleton pattern.
 *
 * <p>Writes to both {@code System.out} and a rolling log file under
 * {@code logs/bank.log}.  No external libraries required.</p>
 */
public final class BankLogger {

    private static final BankLogger INSTANCE = new BankLogger();

    public enum Level { DEBUG, INFO, WARN, ERROR }

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final String logFile = "logs/bank.log";
    private Level minimumLevel = Level.INFO;

    private BankLogger() {
        // Ensure log directory exists
        try { Files.createDirectories(Paths.get("logs")); }
        catch (IOException ignored) {}
    }

    public static BankLogger getInstance() { return INSTANCE; }

    // ── Log level ──────────────────────────────────────────────────────────────
    public void setMinimumLevel(Level level) { this.minimumLevel = level; }

    // ── Logging methods ────────────────────────────────────────────────────────
    public void debug(String message) { log(Level.DEBUG, message); }
    public void info(String message)  { log(Level.INFO,  message); }
    public void warn(String message)  { log(Level.WARN,  message); }
    public void error(String message) { log(Level.ERROR, message); }

    public void error(String message, Throwable t) {
        log(Level.ERROR, message + " | " + t.getClass().getSimpleName() + ": " + t.getMessage());
    }

    private synchronized void log(Level level, String message) {
        if (level.ordinal() < minimumLevel.ordinal()) return;

        String entry = String.format("[%s] [%-5s] %s%n",
                LocalDateTime.now().format(FMT), level, message);

        // Console
        if (level == Level.ERROR || level == Level.WARN)
            System.err.print(entry);
        else
            System.out.print(entry);

        // File
        try (FileWriter fw = new FileWriter(logFile, true)) {
            fw.write(entry);
        } catch (IOException ignored) {}
    }
}
