package com.plating.erp.message.service;

/**
 * 邮件发送服务
 */
public interface EmailService {
    
    /**
     * 异步发送邮件并记录
     * @param tenantId 租户ID
     * @param noticeId 关联消息ID(可为null)
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendEmailAsync(Long tenantId, Long noticeId, String toEmail, String subject, String content);
    
    /**
     * 发送新用户初始化密码邮件
     * @param userId 用户ID
     * @param email 用户邮箱
     * @param realName 用户姓名
     * @param username 用户名
     * @param plainPassword 明文密码
     */
    void sendNewUserPasswordEmail(Long userId, String email, String realName, String username, String plainPassword);
    
    /**
     * 发送待办通知邮件
     * @param userId 用户ID
     * @param email 用户邮箱
     * @param realName 用户姓名
     * @param todoTitle 待办标题
     * @param todoType 待办类型
     * @param priority 优先级
     * @param content 待办内容
     */
    void sendTodoNotificationEmail(Long userId, String email, String realName, 
                                   String todoTitle, String todoType, Integer priority, String content);
    
    /**
     * 发送消息通知邮件
     * @param userId 用户ID
     * @param email 用户邮箱
     * @param realName 用户姓名
     * @param messageTitle 消息标题
     * @param messageType 消息类型
     * @param content 消息内容
     */
    void sendMessageNotificationEmail(Long userId, String email, String realName,
                                      String messageTitle, String messageType, String content);
}
