package com.bank.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a bank customer.  One customer may hold multiple accounts.
 *
 * <p>Built via the {@link Builder} pattern so that optional fields like
 * phone number and email can be added after the mandatory ones.</p>
 */
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String     customerId;
    private final String     firstName;
    private final String     lastName;
    private final LocalDate  dateOfBirth;
    private       String     email;
    private       String     phone;
    private       String     address;
    private final LocalDate  customerSince;
    private       boolean    active;
    private final List<String> accountNumbers; // linked accounts

    private Customer(Builder builder) {
        this.customerId     = builder.customerId;
        this.firstName      = builder.firstName;
        this.lastName       = builder.lastName;
        this.dateOfBirth    = builder.dateOfBirth;
        this.email          = builder.email;
        this.phone          = builder.phone;
        this.address        = builder.address;
        this.customerSince  = LocalDate.now();
        this.active         = true;
        this.accountNumbers = new ArrayList<>();
    }

    // ── Getters / Setters ──────────────────────────────────────────────────────
    public String     getCustomerId()    { return customerId; }
    public String     getFirstName()     { return firstName; }
    public String     getLastName()      { return lastName; }
    public String     getFullName()      { return firstName + " " + lastName; }
    public LocalDate  getDateOfBirth()   { return dateOfBirth; }
    public String     getEmail()         { return email; }
    public String     getPhone()         { return phone; }
    public String     getAddress()       { return address; }
    public LocalDate  getCustomerSince() { return customerSince; }
    public boolean    isActive()         { return active; }
    public List<String> getAccountNumbers() { return Collections.unmodifiableList(accountNumbers); }

    public void setEmail(String email)     { this.email = email; }
    public void setPhone(String phone)     { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setActive(boolean active)  { this.active = active; }

    public void linkAccount(String accountNumber) {
        if (!accountNumbers.contains(accountNumber))
            accountNumbers.add(accountNumber);
    }

    public int getAge() {
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        return Objects.equals(customerId, ((Customer) o).customerId);
    }

    @Override
    public int hashCode() { return Objects.hash(customerId); }

    @Override
    public String toString() {
        return String.format("Customer[%s | %s | Age:%d | Accounts:%d]",
                customerId, getFullName(), getAge(), accountNumbers.size());
    }

    // ── Builder ────────────────────────────────────────────────────────────────
    public static class Builder {
        private final String    customerId;
        private final String    firstName;
        private final String    lastName;
        private final LocalDate dateOfBirth;
        private String email   = "";
        private String phone   = "";
        private String address = "";

        public Builder(String customerId, String firstName, String lastName, LocalDate dateOfBirth) {
            this.customerId  = Objects.requireNonNull(customerId, "customerId required");
            this.firstName   = Objects.requireNonNull(firstName,  "firstName required");
            this.lastName    = Objects.requireNonNull(lastName,   "lastName required");
            this.dateOfBirth = Objects.requireNonNull(dateOfBirth,"dateOfBirth required");
        }

        public Builder email(String email)     { this.email   = email;   return this; }
        public Builder phone(String phone)     { this.phone   = phone;   return this; }
        public Builder address(String address) { this.address = address; return this; }

        public Customer build() { return new Customer(this); }
    }
}
