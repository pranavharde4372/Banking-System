package com.bank.model;

import java.math.BigDecimal;

import jakarta.persistence.Embeddable;

@Embeddable 
public class TransactionRecord {
    
    public String date;
    public String type;
    public BigDecimal amount;
    public String description;

    public TransactionRecord() {}

    public TransactionRecord(String date, String type, BigDecimal amount, String description) {
        this.date = date;
        this.type = type;
        this.amount = amount;
        this.description = description;
    }
}