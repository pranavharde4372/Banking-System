# 🏦 Java Banking System Simulation

A **production-quality** banking system built with Java 17, demonstrating
advanced language features, design patterns, and enterprise-grade architecture.

---

## 📋 Table of Contents
1. [Project Overview](#-project-overview)
2. [Architecture](#-architecture)
3. [Features](#-features)
4. [Design Patterns](#-design-patterns)
5. [Java 8+ Features](#-java-8-features)
6. [Project Structure](#-project-structure)
7. [Getting Started](#-getting-started)
8. [Running Tests](#-running-tests)
9. [Sample Output](#-sample-output)
10. [Technical Deep-Dive](#-technical-deep-dive)

---

## 🎯 Project Overview

This simulation models a complete retail banking system including:

| Domain | Capabilities |
|---|---|
| Account Management | Savings, Current (with overdraft), Fixed Deposits |
| Transactions | Deposit, Withdraw, Transfer, Interest, Fees |
| Loans | Personal, Home, Vehicle, Education with EMI amortization |
| Reporting | Portfolio analysis, transaction summaries, interest projections |
| Infrastructure | Logging, performance monitoring, file persistence, audit trail |

---

## 🏗️ Architecture

```
Presentation Layer  →  Main.java (demo runner)
                        BankingSystemTest.java (test suite)

Service Layer       →  BankingService (core facade / singleton)
                        ReportService  (analytics & reporting)

Domain Layer        →  Account hierarchy (abstract + 3 concrete)
                        Transaction (immutable, Builder pattern)
                        Customer   (Builder pattern)
                        Loan       (EMI amortization)

Strategy Layer      →  InterestStrategy interface + 3 implementations

Observer Layer      →  TransactionObserver interface
                        AuditObserver, FraudDetectionObserver

Factory Layer       →  AccountFactory (Factory + Singleton)

Infrastructure      →  BankLogger    (Singleton)
                        FileManager   (Singleton, serialization)
                        PerformanceMonitor (Singleton, ConcurrentHashMap)

Exception Layer     →  BankingException hierarchy (5 classes)
```

---

## ✨ Features

### Banking Operations (10+)
1. **Deposit** – credit any account with validation
2. **Withdraw** – debit with type-specific rules (minimum balance, monthly limit)
3. **Transfer** – atomic debit/credit between accounts (deadlock-free)
4. **Open Account** – Savings / Current / Fixed Deposit
5. **Close Account** – soft delete with audit trail
6. **Freeze/Unfreeze** – compliance control
7. **Apply Interest** – bulk, strategy-driven interest crediting
8. **Apply for Loan** – eligibility check + disbursement
9. **Repay Loan (EMI)** – amortized principal/interest split
10. **Premature FD Closure** – penalty calculation + settlement
11. **Generate Statement** – formatted transaction history
12. **Analytics Queries** – high-value accounts, statistics, top transactions

---

## 🎨 Design Patterns

| Pattern | Where Used | Purpose |
|---|---|---|
| **Singleton** | `BankingService`, `AccountFactory`, `BankLogger`, `FileManager`, `PerformanceMonitor` | Single instance per JVM |
| **Factory Method** | `AccountFactory.createAccount(type, ...)` | Decouple account construction from client code |
| **Strategy** | `InterestStrategy` → `SavingsInterestStrategy`, `CurrentInterestStrategy`, `FixedDepositInterestStrategy` | Swap interest formulas without changing accounts |
| **Observer** | `TransactionObserver` → `AuditObserver`, `FraudDetectionObserver` | Decouple event producers from consumers |
| **Builder** | `Transaction.Builder`, `Customer.Builder` | Readable, validated object construction |
| **Template Method** | `Account.withdraw()` calls abstract `validateWithdrawal()` | Common algorithm skeleton, subclass-specific rules |
| **Facade** | `BankingService` | Single entry point hiding subsystem complexity |

---

## ☕ Java 8+ Features

| Feature | Example in Code |
|---|---|
| **Lambda Expressions** | `accounts.values().stream().filter(a -> a.getBalance() > threshold)` |
| **Stream API** | `getHighValueAccounts()`, `getAccountsByType()`, `getTotalVolumeByType()` |
| **Optional** | `findAccount()`, `findCustomer()`, `findLoan()` – null-safe lookups |
| **Collectors** | `groupingBy`, `summarizingDouble`, `partitioningBy` in `ReportService` |
| **Method References** | `Comparator.comparingDouble(Account::getBalance).reversed()` |
| **DateTime API** | `LocalDateTime` for transaction timestamps; `LocalDate` for maturity |
| **Switch Expression** | `AccountFactory.createAccount()` dispatch |
| **var keyword** | `var stats = bank.getBalanceStatistics()` |
| **DoubleSummaryStatistics** | `getBalanceStatistics()` – count/min/max/avg in one pass |

---

## 📁 Project Structure

```
BankingSystem/
├── pom.xml                                 Maven build file
├── README.md
├── data/                                   Serialized account/loan data
│   └── backups/                            Auto-created before each save
├── logs/
│   └── bank.log                            Rolling application log
└── src/
    ├── main/java/com/bank/
    │   ├── Main.java                        ← Entry point (full demo)
    │   ├── exception/
    │   │   ├── BankingException.java        Base exception
    │   │   ├── InsufficientFundsException
    │   │   ├── AccountNotFoundException
    │   │   ├── InvalidTransactionException
    │   │   └── LoanException
    │   ├── model/
    │   │   ├── AccountType.java             Enum (SAVINGS/CURRENT/FIXED_DEPOSIT)
    │   │   ├── TransactionType.java         Enum (DEPOSIT/WITHDRAWAL/…)
    │   │   ├── Account.java                 Abstract base
    │   │   ├── SavingsAccount.java          Monthly withdrawal limit
    │   │   ├── CurrentAccount.java          Overdraft facility
    │   │   ├── FixedDepositAccount.java     Tenure + premature closure
    │   │   ├── Transaction.java             Immutable + Builder
    │   │   ├── Customer.java                Builder pattern
    │   │   └── Loan.java                    EMI amortization + LoanPayment
    │   ├── service/
    │   │   └── BankingService.java          Core facade (Singleton)
    │   ├── strategy/
    │   │   ├── InterestStrategy.java        Interface
    │   │   ├── SavingsInterestStrategy.java 4% simple
    │   │   ├── CurrentInterestStrategy.java 0.5% simple
    │   │   └── FixedDepositInterestStrategy 6.5% compound quarterly
    │   ├── factory/
    │   │   └── AccountFactory.java          Factory + Singleton
    │   ├── observer/
    │   │   ├── TransactionObserver.java     Interface
    │   │   ├── BankingEvent.java            Event model
    │   │   ├── AuditObserver.java           Logs all events
    │   │   └── FraudDetectionObserver.java  Flags large transactions
    │   ├── report/
    │   │   └── ReportService.java           Analytics (Streams)
    │   └── util/
    │       ├── BankLogger.java              Singleton logger
    │       ├── FileManager.java             Serialization + backups
    │       └── PerformanceMonitor.java      ConcurrentHashMap metrics
    └── test/java/com/bank/
        └── BankingSystemTest.java           Self-contained test suite
```

---

## 🚀 Getting Started

### Prerequisites
- Java 17+ (`java -version`)
- Maven 3.8+ (`mvn -version`)

### Build & Run

```bash
# Clone / unzip the project
cd BankingSystem

# Compile
mvn compile

# Run the full demo
mvn exec:java -Dexec.mainClass="com.bank.Main"

# Run the test suite
mvn exec:java -Dexec.mainClass="com.bank.BankingSystemTest"

# Build an executable JAR
mvn package
java -jar target/banking-system.jar
```

---

## 🧪 Running Tests

The test suite is self-contained (no JUnit required):

```
✅ PASS  Customer is created with correct ID
✅ PASS  Customer full name is set
✅ PASS  Duplicate customer ID throws exception
✅ PASS  Savings account created with correct balance
✅ PASS  Below-minimum deposit throws exception
✅ PASS  Balance increases after deposit
✅ PASS  Zero deposit throws exception
✅ PASS  Balance decreases after withdrawal
✅ PASS  Source balance decreases
✅ PASS  Target balance increases
✅ PASS  Self-transfer throws exception
✅ PASS  Savings interest ~4% p.a.
✅ PASS  Loan is active
✅ PASS  EMI is positive
✅ PASS  Premature FD withdrawal blocked
✅ PASS  Balance correct after 500 concurrent deposits
✅ PASS  Optional.isPresent for valid account
✅ PASS  High value stream returns accounts above threshold
...

Results: 40 passed, 0 failed
```

---

## 📊 Sample Output

```
🏦  JAVA BANKING SYSTEM – SUMMARY REPORT
═══════════════════════════════════════════════
  Total Accounts         :      6
  Active Customers       :      3
  Active Loans           :      1
  Total Deposits         : ₹2,92,458.90
  Total Transactions     :     42

  ACCOUNTS BY TYPE:
  Savings Account      :   3 accounts  ₹72,458.90 total
  Current Account      :   2 accounts  ₹45,000.00 total
  Fixed Deposit        :   1 accounts  ₹1,75,000.00 total

  BALANCE STATISTICS:
  Min   : ₹9,458.90
  Max   : ₹1,75,000.00
  Avg   : ₹48,743.15
  Total : ₹2,92,458.90
```

---

## 🔍 Technical Deep-Dive

### Thread Safety
`Account.deposit()` and `Account.withdraw()` are `synchronized`.  
`BankingService.transfer()` acquires locks in a consistent lexicographic order
to prevent deadlock while maintaining atomicity.

### Interest Formula
- **Savings/Current** – Simple interest: `I = P × r × t/365`
- **Fixed Deposit**   – Quarterly compound: `A = P(1 + r/4)^(4t)`

### EMI Amortization
Standard reducing-balance formula:  
`EMI = P × r × (1+r)^n / [(1+r)^n – 1]`  
where `r = monthly rate`, `n = tenure months`.

### Data Persistence
Java object serialization via `ObjectOutputStream`.  A timestamped `.bak` copy
is created before every write so that a failed write never corrupts existing data.

---

## 📄 License

MIT License – free to use, modify, and distribute.
