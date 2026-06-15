package com.bank.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bank.model.Account;
import com.bank.service.BankingService;

@RestController
@RequestMapping("/api")
@CrossOrigin("*") 
public class BankingController {

    private final BankingService bankingService;

    public BankingController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<Account>> getAllAccounts() { return ResponseEntity.ok(bankingService.getAllAccounts()); }

    @GetMapping("/accounts/{accNo}/history")
    public ResponseEntity<?> getHistory(@PathVariable String accNo) {
        Account acc = bankingService.getAccount(accNo);
        if (acc != null) return ResponseEntity.ok(acc.getHistory());
        return ResponseEntity.status(404).body("Account not found");
    }

    @PostMapping("/create")
    public ResponseEntity<String> createAccount(@RequestBody AccountRequest req) {
        if (req.accNo == null || req.accNo.trim().isEmpty()) return ResponseEntity.badRequest().body("Account Number is required");
        if (req.firstName == null || req.firstName.trim().isEmpty()) return ResponseEntity.badRequest().body("First Name is required");
        if (req.middleName == null || req.middleName.trim().isEmpty()) return ResponseEntity.badRequest().body("Middle Name is required");
        if (req.lastName == null || req.lastName.trim().isEmpty()) return ResponseEntity.badRequest().body("Last Name is required");
        if (req.dob == null || req.dob.trim().isEmpty()) return ResponseEntity.badRequest().body("Date of Birth is required");
        if (req.phone == null || req.phone.trim().isEmpty()) return ResponseEntity.badRequest().body("Phone Number is required");
        if (req.aadhar == null || req.aadhar.trim().isEmpty()) return ResponseEntity.badRequest().body("Aadhar Number is required");
        if (req.pan == null || req.pan.trim().isEmpty()) return ResponseEntity.badRequest().body("PAN Number is required");
        if (req.balance == null) return ResponseEntity.badRequest().body("Initial Balance is required");
        if (req.type == null || req.type.trim().isEmpty()) return ResponseEntity.badRequest().body("Account Type is required");

        try {
            bankingService.addAccount(new Account(
                req.accNo, req.firstName, req.middleName, req.lastName, 
                req.phone, req.aadhar, req.pan, req.dob, 
                req.balance, req.type
            ));
            return ResponseEntity.ok("Account Created Successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestBody TransactionRequest req) {
        try { bankingService.deposit(req.accNo, req.amount); return ResponseEntity.ok("Deposited Successfully"); } 
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<String> withdraw(@RequestBody TransactionRequest req) {
        try { bankingService.withdraw(req.accNo, req.amount); return ResponseEntity.ok("Withdrawn Successfully"); } 
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody TransferRequest req) {
        try { bankingService.transfer(req.from, req.to, req.amount); return ResponseEntity.ok("Transfer Completed Successfully"); } 
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @DeleteMapping("/accounts/{accNo}")
    public ResponseEntity<String> deleteAccount(@PathVariable String accNo) {
        try { bankingService.deleteAccount(accNo); return ResponseEntity.ok("Account Deleted Successfully"); } 
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest req) {
        if (bankingService.registerUser(req.username, req.password, req.email, req.phone)) {
            return ResponseEntity.ok("SUCCESS");
        }
        return ResponseEntity.status(409).body("Username already exists");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthRequest req) {
        if (bankingService.validateUser(req.username, req.password)) return ResponseEntity.ok("SUCCESS");
        return ResponseEntity.status(401).body("Invalid Credentials");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        if (bankingService.resetPassword(req.username, req.email, req.phone, req.newPassword)) {
            return ResponseEntity.ok("Password reset successfully. You can now login!");
        }
        return ResponseEntity.status(401).body("Verification failed.");
    }

    public static class AccountRequest { public String accNo; public String firstName; public String middleName; public String lastName; public String phone; public String aadhar; public String pan; public String dob; public BigDecimal balance; public String type; }
    public static class TransactionRequest { public String accNo; public BigDecimal amount; }
    public static class TransferRequest { public String from; public String to; public BigDecimal amount; }
    public static class AuthRequest { public String username; public String password; }
    public static class RegisterRequest { public String username; public String password; public String email; public String phone; }
    public static class ForgotPasswordRequest { public String username; public String email; public String phone; public String newPassword; }
}