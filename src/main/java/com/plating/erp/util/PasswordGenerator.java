package com.plating.erp.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码哈希生成工具
 * 
 * 用于生成正确的 BCrypt 密码哈希值，可用于：
 * 1. 数据库迁移脚本中的初始密码
 * 2. 手动重置用户密码
 * 3. 测试账号密码生成
 */
public class PasswordGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        
        // 生成系统管理员密码
        String systemPassword = "System@123456";
        String systemHash = encoder.encode(systemPassword);
        System.out.println("========================================");
        System.out.println("系统管理员密码哈希");
        System.out.println("明文密码: " + systemPassword);
        System.out.println("BCrypt哈希: " + systemHash);
        System.out.println("========================================");
        
        // 生成租户管理员密码
        String adminPassword = "123456";
        String adminHash = encoder.encode(adminPassword);
        System.out.println("\n========================================");
        System.out.println("租户管理员密码哈希");
        System.out.println("明文密码: " + adminPassword);
        System.out.println("BCrypt哈希: " + adminHash);
        System.out.println("========================================");
        
        // 生成测试用户密码
        String testPassword = "Test@123456";
        String testHash = encoder.encode(testPassword);
        System.out.println("\n========================================");
        System.out.println("测试用户密码哈希");
        System.out.println("明文密码: " + testPassword);
        System.out.println("BCrypt哈希: " + testHash);
        System.out.println("========================================");
        
        // 生成 SQL 更新语句
        System.out.println("\n========================================");
        System.out.println("SQL 更新语句");
        System.out.println("========================================");
        System.out.println("UPDATE sys_user SET password_hash = '" + systemHash + "' WHERE username = 'system';");
        System.out.println("UPDATE sys_user SET password_hash = '" + adminHash + "' WHERE username = 'a-admin';");
        System.out.println("UPDATE sys_user SET password_hash = '" + testHash + "' WHERE username = 'a-user';");
        System.out.println("========================================");
    }
}
