package com.bank.util;

import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Lightweight performance monitor – records operation durations in nanoseconds
 * and exposes summary statistics.
 *
 * <p>Thread-safe: uses {@link ConcurrentHashMap} and {@link LongAdder} so that
 * multiple concurrent banking operations do not block each other.</p>
 */
public final class PerformanceMonitor {

    private static final PerformanceMonitor INSTANCE = new PerformanceMonitor();
    private PerformanceMonitor() {}
    public static PerformanceMonitor getInstance() { return INSTANCE; }

    private final Map<String, LongAdder> callCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> totalNanos  = new ConcurrentHashMap<>();

    /**
     * Records one measurement for {@code operation}.
     *
     * @param operation  logical operation name (e.g. "deposit", "transfer")
     * @param nanos      elapsed nanoseconds
     */
    public void record(String operation, long nanos) {
        callCounts.computeIfAbsent(operation, k -> new LongAdder()).increment();
        totalNanos .computeIfAbsent(operation, k -> new LongAdder()).add(nanos);
    }

    /** Returns average duration in milliseconds for the given operation. */
    public double averageMs(String operation) {
        LongAdder count = callCounts.get(operation);
        LongAdder total = totalNanos.get(operation);
        if (count == null || count.sum() == 0) return 0.0;
        return (total.sum() / 1_000_000.0) / count.sum();
    }

    /** Returns total call count for the given operation. */
    public long callCount(String operation) {
        LongAdder adder = callCounts.get(operation);
        return adder == null ? 0L : adder.sum();
    }

    /** Prints a summary report to stdout. */
    public void printReport() {
        System.out.println("\n⚡ PERFORMANCE METRICS");
        System.out.println("─".repeat(55));
        System.out.printf("  %-20s  %8s  %10s%n", "Operation", "Calls", "Avg (ms)");
        System.out.println("  " + "─".repeat(50));
        callCounts.keySet().stream().sorted().forEach(op ->
                System.out.printf("  %-20s  %8d  %10.3f%n",
                        op, callCount(op), averageMs(op))
        );
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        System.out.printf("%n  Memory in use : %d MB%n", usedMb);
        System.out.printf("  Available CPUs: %d%n", rt.availableProcessors());
    }
}
