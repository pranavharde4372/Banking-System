package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

@Entity 
public class Account {
    
    @Id 
    private String accountNumber;
    
    @Column(nullable = false) private String firstName;
    @Column(nullable = false) private String middleName;
    @Column(nullable = false) private String lastName;
    @Column(nullable = false) private String phone;
    @Column(nullable = false) private String aadharNumber;
    @Column(nullable = false) private String panNumber;
    @Column(nullable = false) private String dob; 
    @Column(nullable = false) private BigDecimal balance;
    @Column(nullable = false) private String type;
    
    @ElementCollection(fetch = FetchType.EAGER) 
    private List<TransactionRecord> history = new ArrayList<>();

    public Account() {} 

    public Account(String accNo, String fName, String mName, String lName, 
                   String phone, String aadhar, String pan, String dob, 
                   BigDecimal balance, String type) {
        this.accountNumber = accNo;
        this.firstName = fName;
        this.middleName = mName;
        this.lastName = lName;
        this.phone = phone;
        this.aadharNumber = aadhar;
        this.panNumber = pan;
        this.dob = dob;
        this.balance = balance;
        this.type = type;
        
        recordTransaction("OPENING_BALANCE", balance, "Account Opened");
    }

    public String getAccountNumber() { return accountNumber; }
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getAadharNumber() { return aadharNumber; }
    public String getPanNumber() { return panNumber; }
    public String getDob() { return dob; }
    public BigDecimal getBalance() { return balance; }
    public String getType() { return type; }
    public List<TransactionRecord> getHistory() { return history; }

    public synchronized void deposit(BigDecimal amt, String description) {
        this.balance = this.balance.add(amt);
        recordTransaction("DEPOSIT", amt, description);
    }

    public synchronized void withdraw(BigDecimal amt, String description) {
        if (this.balance.compareTo(amt) >= 0) {
            this.balance = this.balance.subtract(amt);
            recordTransaction("WITHDRAWAL", amt, description);
        } else {
            throw new RuntimeException("Insufficient funds");
        }
    }

    private void recordTransaction(String type, BigDecimal amount, String desc) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.history.add(new TransactionRecord(time, type, amount, desc));
    }
}