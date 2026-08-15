package com.plating.erp.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP地址工具类
 * 
 * 参考主流产品（阿里巴巴、腾讯、钉钉）的IP获取标准实现
 * 支持多级代理、负载均衡场景下的真实IP获取
 * 
 * @author Plating ERP Team
 */
public final class IpUtils {
    
    private static final Logger log = LoggerFactory.getLogger(IpUtils.class);
    
    /** 未知IP标识 */
    private static final String UNKNOWN = "unknown";
    
    /** 本地回环地址 */
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IPV6_SHORT = "::1";
    
    /** 内网IP段 */
    private static final String[] INNER_IP_SEGMENTS = {
            "10.",      // 10.0.0.0/8
            "172.16.",  // 172.16.0.0/12
            "172.17.",
            "172.18.",
            "172.19.",
            "172.20.",
            "172.21.",
            "172.22.",
            "172.23.",
            "172.24.",
            "172.25.",
            "172.26.",
            "172.27.",
            "172.28.",
            "172.29.",
            "172.30.",
            "172.31.",
            "192.168."  // 192.168.0.0/16
    };
    
    private IpUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }
    
    /**
     * 获取客户端真实IP地址
     * 
     * 优先级顺序：
     * 1. X-Real-IP (Nginx代理)
     * 2. X-Forwarded-For (标准代理头)
     * 3. Proxy-Client-IP (Apache)
     * 4. WL-Proxy-Client-IP (WebLogic)
     * 5. HTTP_CLIENT_IP (某些代理)
     * 6. HTTP_X_FORWARDED_FOR (某些代理)
     * 7. request.getRemoteAddr() (直连)
     * 
     * @param request HTTP请求
     * @return 客户端真实IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        
        String ip = null;
        
        // 1. 优先获取 Nginx 代理的真实IP
        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            log.debug("从 X-Real-IP 获取到IP: {}", ip);
            return normalizeIp(ip);
        }
        
        // 2. 获取标准代理头 X-Forwarded-For
        ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            log.debug("从 X-Forwarded-For 获取到IP: {}", ip);
            // X-Forwarded-For 可能包含多个IP：client, proxy1, proxy2
            // 第一个IP才是真实的客户端IP
            return getFirstIp(ip);
        }
        
        // 3. 获取 Apache 代理IP
        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) {
            log.debug("从 Proxy-Client-IP 获取到IP: {}", ip);
            return normalizeIp(ip);
        }
        
        // 4. 获取 WebLogic 代理IP
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) {
            log.debug("从 WL-Proxy-Client-IP 获取到IP: {}", ip);
            return normalizeIp(ip);
        }
        
        // 5. 获取其他代理头
        ip = request.getHeader("HTTP_CLIENT_IP");
        if (isValidIp(ip)) {
            log.debug("从 HTTP_CLIENT_IP 获取到IP: {}", ip);
            return normalizeIp(ip);
        }
        
        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (isValidIp(ip)) {
            log.debug("从 HTTP_X_FORWARDED_FOR 获取到IP: {}", ip);
            return getFirstIp(ip);
        }
        
        // 6. 获取直连IP
        ip = request.getRemoteAddr();
        if (isValidIp(ip)) {
            log.debug("从 RemoteAddr 获取到IP: {}", ip);
            return normalizeIp(ip);
        }
        
        log.warn("无法获取客户端IP，返回 unknown");
        return UNKNOWN;
    }
    
    /**
     * 验证IP地址是否有效
     */
    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip);
    }
    
    /**
     * 获取第一个IP地址（用于X-Forwarded-For等多IP场景）
     */
    private static String getFirstIp(String ips) {
        if (ips.contains(",")) {
            String firstIp = ips.split(",")[0].trim();
            log.debug("从多个IP中提取第一个: {}", firstIp);
            return normalizeIp(firstIp);
        }
        return normalizeIp(ips);
    }
    
    /**
     * 标准化IP地址
     * - 去除首尾空格
     * - IPv6本地回环转换为IPv4
     * - 验证IP格式
     */
    private static String normalizeIp(String ip) {
        if (ip == null) {
            return UNKNOWN;
        }
        
        ip = ip.trim();
        
        // IPv6本地回环转换为IPv4
        if (LOCALHOST_IPV6.equals(ip) || LOCALHOST_IPV6_SHORT.equals(ip)) {
            return LOCALHOST_IPV4;
        }
        
        // 验证IP格式（简单验证）
        if (!ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$") && 
            !ip.matches("^[0-9a-fA-F:]+$")) {
            log.warn("IP格式异常: {}", ip);
            return UNKNOWN;
        }
        
        return ip;
    }
    
    /**
     * 判断是否为内网IP
     * 
     * @param ip IP地址
     * @return true-内网IP，false-外网IP
     */
    public static boolean isInnerIp(String ip) {
        if (ip == null || UNKNOWN.equals(ip)) {
            return false;
        }
        
        // 本地回环地址
        if (LOCALHOST_IPV4.equals(ip) || LOCALHOST_IPV6.equals(ip) || LOCALHOST_IPV6_SHORT.equals(ip)) {
            return true;
        }
        
        // 检查内网IP段
        for (String segment : INNER_IP_SEGMENTS) {
            if (ip.startsWith(segment)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取服务器本机IP地址
     * 
     * @return 本机IP地址
     */
    public static String getLocalIp() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String ip = inetAddress.getHostAddress();
            log.debug("获取到本机IP: {}", ip);
            return ip;
        } catch (UnknownHostException e) {
            log.error("获取本机IP失败", e);
            return LOCALHOST_IPV4;
        }
    }
    
    /**
     * 获取客户端IP地址（带内网IP识别）
     * 
     * 返回格式：真实IP 或 "真实IP(内网)"
     * 
     * @param request HTTP请求
     * @return 格式化的IP地址
     */
    public static String getClientIpWithFlag(HttpServletRequest request) {
        String ip = getClientIp(request);
        if (isInnerIp(ip)) {
            return ip + "(内网)";
        }
        return ip;
    }
}
