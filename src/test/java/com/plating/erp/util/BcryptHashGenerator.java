package com.plating.erp.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String[] passwords = {"System@123456", "Admin@123456", "User@123456"};
        for (String pwd : passwords) {
            System.out.println(pwd + " -> " + encoder.encode(pwd));
        }
    }
}
