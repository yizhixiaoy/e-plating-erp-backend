package com.plating.erp.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BcryptHashGeneratorTest {
    @Test
    void generateHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String[] passwords = {"System@123456", "Admin@123456", "User@123456"};
        for (String pwd : passwords) {
            String ss = encoder.encode(pwd);
            System.out.println(pwd + " -> " + encoder.encode(ss));
            System.out.println("chckpwd -> " + encoder.matches(pwd, ss));
        }
    }
}
