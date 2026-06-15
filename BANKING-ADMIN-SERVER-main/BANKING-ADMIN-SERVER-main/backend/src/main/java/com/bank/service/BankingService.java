package com.bank.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bank.model.Account;
import com.bank.model.User;
import com.bank.repository.AccountRepository;
import com.bank.repository.UserRepository;

@Service
public class BankingService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    public void addAccount(Account a) { accountRepository.save(a); }
    public List<Account> getAllAccounts() { return accountRepository.findAll(); }
    public Account getAccount(String accNo) { return accountRepository.findById(accNo).orElse(null); }

    public void deposit(String accNo, BigDecimal amt) {
        Account acc = accountRepository.findById(accNo).orElseThrow(() -> new RuntimeException("Account not found"));
        acc.deposit(amt, "Cash/Online Deposit");
        accountRepository.save(acc); 
    }

    public void withdraw(String accNo, BigDecimal amt) {
        Account acc = accountRepository.findById(accNo).orElseThrow(() -> new RuntimeException("Account not found"));
        acc.withdraw(amt, "Cash/Online Withdrawal");
        accountRepository.save(acc); 
    }

    public void transfer(String from, String to, BigDecimal amt) {
        Account a1 = accountRepository.findById(from).orElseThrow(() -> new RuntimeException("Source account not found"));
        Account a2 = accountRepository.findById(to).orElseThrow(() -> new RuntimeException("Destination account not found"));
        
        a1.withdraw(amt, "Transfer to " + to);
        a2.deposit(amt, "Transfer from " + from);
        
        accountRepository.save(a1);
        accountRepository.save(a2);
    }

    public void deleteAccount(String accNo) {
        if (!accountRepository.existsById(accNo)) throw new RuntimeException("Account not found");
        accountRepository.deleteById(accNo);
    }

    public boolean registerUser(String u, String p, String email, String phone) {
        if (userRepository.existsById(u)) return false; 
        userRepository.save(new User(u, p, email, phone));
        return true;
    }

    public boolean validateUser(String u, String p) {
        User user = userRepository.findById(u).orElse(null);
        if (user == null) return false;
        return user.getPassword().equals(p);
    }

    public boolean resetPassword(String u, String email, String phone, String newPassword) {
        User user = userRepository.findById(u).orElse(null);
        if (user == null) return false;
        if (user.getEmail().equals(email) && user.getPhoneNumber().equals(phone)) {
            user.setPassword(newPassword);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}