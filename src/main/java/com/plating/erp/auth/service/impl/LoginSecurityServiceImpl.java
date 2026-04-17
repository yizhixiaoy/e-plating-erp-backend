package com.plating.erp.auth.service.impl;

import com.plating.erp.auth.service.LoginSecurityService;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录安全服务实现
 * 
 * 提供两层安全防护机制：
 * 1. 账号级别防护：防止对特定账号的暴力破解（5次失败/15分钟）
 * 2. IP级别防护：防止来自同一IP的批量攻击（10次失败/5分钟，锁定30分钟）
 * 
 * 使用Redis存储尝试次数，支持分布式部署
 */
@Service
public class LoginSecurityServiceImpl implements LoginSecurityService {
    private static final Logger log = LoggerFactory.getLogger(LoginSecurityServiceImpl.class);
    
    private final RedisTemplate<String, Integer> redisTemplate;
    
    // 账号级别防护配置
    private static final int ACCOUNT_MAX_ATTEMPTS = 5;      // 最大尝试次数
    private static final int ACCOUNT_LOCK_MINUTES = 15;     // 锁定时间（分钟）
    private static final String ACCOUNT_KEY_PREFIX = "login:attempt:";
    
    // IP级别防护配置
    private static final int IP_MAX_ATTEMPTS = 10;          // 最大尝试次数
    private static final int IP_WINDOW_MINUTES = 5;         // 统计窗口（分钟）
    private static final int IP_LOCK_MINUTES = 30;          // 锁定时间（分钟）
    private static final String IP_KEY_PREFIX = "login:brute:";

    public LoginSecurityServiceImpl(@Qualifier("redisTemplateForInteger") RedisTemplate<String, Integer> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检查账号登录尝试次数
     * 用于防止针对特定账号的暴力破解攻击
     * 
     * 规则：
     * - 同一账号在同一租户下5分钟内失败5次，锁定15分钟
     * - 成功登录后清除计数
     * 
     * @param username 用户名
     * @param tenantCode 租户编码
     * @throws BizException 超过尝试次数限制时抛出异常
     */
    @Override
    public void checkLoginAttempt(String username, String tenantCode) {
        String key = ACCOUNT_KEY_PREFIX + tenantCode + ":" + username;
        Integer attempts = redisTemplate.opsForValue().get(key);
        
        if (attempts != null && attempts >= ACCOUNT_MAX_ATTEMPTS) {
            log.warn("账号登录尝试次数超限, username={}, tenantCode={}, attempts={}", 
                    username, tenantCode, attempts);
            throw new BizException(ErrorCode.TOO_MANY_ATTEMPTS, 
                    "登录失败次数过多，请" + ACCOUNT_LOCK_MINUTES + "分钟后再试");
        }
        
        if (attempts != null) {
            log.debug("账号登录尝试检查, username={}, tenantCode={}, currentAttempts={}", 
                    username, tenantCode, attempts);
        }
    }

    /**
     * 记录账号登录尝试结果
     * 
     * @param username 用户名
     * @param tenantCode 租户编码
     * @param success 是否登录成功
     */
    @Override
    public void recordLoginAttempt(String username, String tenantCode, boolean success) {
        String key = ACCOUNT_KEY_PREFIX + tenantCode + ":" + username;
        
        if (success) {
            // 登录成功，清除失败计数
            Boolean deleted = redisTemplate.delete(key);
            if (deleted) {
                log.debug("登录成功，清除账号失败计数, username={}, tenantCode={}", username, tenantCode);
            }
        } else {
            // 登录失败，累加计数
            Integer attempts = redisTemplate.opsForValue().get(key);
            if (attempts == null) {
                // 首次失败，设置初始值和过期时间
                redisTemplate.opsForValue().set(key, 1, Duration.ofMinutes(ACCOUNT_LOCK_MINUTES));
                log.debug("账号首次登录失败, username={}, tenantCode={}", username, tenantCode);
            } else {
                // 累加失败次数
                Long newAttempts = redisTemplate.opsForValue().increment(key);
                log.debug("账号登录失败次数累加, username={}, tenantCode={}, attempts={}", 
                        username, tenantCode, newAttempts);
                
                // 达到阈值时，确保过期时间正确设置
                if (attempts >= ACCOUNT_MAX_ATTEMPTS - 1) {
                    redisTemplate.expire(key, Duration.ofMinutes(ACCOUNT_LOCK_MINUTES));
                    log.warn("账号登录失败次数达到阈值, username={}, tenantCode={}, attempts={}", 
                            username, tenantCode, newAttempts);
                }
            }
        }
    }

    /**
     * 检查IP暴力破解攻击
     * 用于防止来自同一IP的批量暴力破解攻击
     * 
     * 规则：
     * - 同一IP在5分钟内失败10次，锁定30分钟
     * - 不区分账号，针对整个IP进行限制
     * 
     * @param ip 客户端IP地址
     * @throws BizException 检测到暴力破解行为时抛出异常
     */
    @Override
    public void checkBruteForce(String ip) {
        String key = IP_KEY_PREFIX + ip;
        Integer attempts = redisTemplate.opsForValue().get(key);
        
        if (attempts != null && attempts >= IP_MAX_ATTEMPTS) {
            log.warn("检测到暴力破解攻击, ip={}, attempts={}", ip, attempts);
            throw new BizException(ErrorCode.BRUTE_FORCE, 
                    "检测到异常登录行为，请" + IP_LOCK_MINUTES + "分钟后再试");
        }
        
        if (attempts != null) {
            log.debug("IP登录尝试检查, ip={}, currentAttempts={}", ip, attempts);
        }
    }

    /**
     * 记录IP登录尝试结果
     * 用于追踪和防范暴力破解攻击
     * 
     * @param ip 客户端IP地址
     * @param success 是否登录成功
     */
    @Override
    public void recordBruteForceAttempt(String ip, boolean success) {
        String key = IP_KEY_PREFIX + ip;
        
        if (!success) {
            // 登录失败，累加IP计数
            Integer attempts = redisTemplate.opsForValue().get(key);
            if (attempts == null) {
                // 首次失败，设置初始值和窗口时间
                redisTemplate.opsForValue().set(key, 1, Duration.ofMinutes(IP_WINDOW_MINUTES));
                log.debug("IP首次登录失败, ip={}", ip);
            } else {
                // 累加失败次数
                Long newAttempts = redisTemplate.opsForValue().increment(key);
                log.debug("IP登录失败次数累加, ip={}, attempts={}", ip, newAttempts);
                
                // 达到阈值时，延长锁定时间
                if (attempts >= IP_MAX_ATTEMPTS - 1) {
                    redisTemplate.expire(key, Duration.ofMinutes(IP_LOCK_MINUTES));
                    log.warn("IP登录失败次数达到阈值，延长锁定时间, ip={}, attempts={}, lockMinutes={}", 
                            ip, newAttempts, IP_LOCK_MINUTES);
                }
            }
        } else {
            // 登录成功不清理IP计数，保留用于安全分析
            // 可以选择在这里记录成功日志
            log.debug("IP登录成功, ip={}", ip);
        }
    }
}
