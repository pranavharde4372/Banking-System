package com.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BankingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
        System.out.println("\n========================================================");
        System.out.println(" ✅ BACKEND RUNNING ON PORT 8080! OPEN YOUR HTML FILE NOW.");
        System.out.println("========================================================\n");
    }
}