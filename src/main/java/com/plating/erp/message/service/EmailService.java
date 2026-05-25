package com.plating.erp.message.service;

/**
 * 邮件发送服务
 */
public interface EmailService {
    
    /**
     * 异步发送邮件并记录
     * @param tenantId 租户ID
     * @param userId 接收人用户ID
     * @param noticeId 关联消息ID(可为null)
     * @param senderEmail 发件人邮箱（实际操作人）
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendEmailAsync(Long tenantId, Long userId, Long noticeId, String senderEmail, String toEmail, String subject, String content);
    
    /**
     * 发送新用户初始化密码邮件
     * @param userId 用户ID（接收人）
     * @param email 用户邮箱
     * @param realName 用户姓名
     * @param username 用户名
     * @param plainPassword 明文密码
     * @param tenantId 租户ID
     * @param creatorId 操作人ID（创建该用户的 admin）
     */
    void sendNewUserPasswordEmail(Long userId, String email, String realName, 
                                  String username, String plainPassword, Long tenantId, Long creatorId);
    
    /**
     * 发送待办通知邮件
     * @param userId 用户ID（接收人）
     * @param email 用户邮箱
     * @param realName 用户姓名
     * @param todoTitle 待办标题
     * @param todoType 待办类型
     * @param priority 优先级
     * @param content 待办内容
     * @param tenantId 租户ID
     * @param relatedId 关联ID（待办/消息ID）
     * @param creatorId 操作人ID（待办创建人）
     */
    void sendTodoNotificationEmail(Long userId, String email, String realName, 
                                   String todoTitle, String todoType, Integer priority, String content,
                                   Long tenantId, Long relatedId, Long creatorId);
    
    /**
     * 发送消息通知邮件
     * @param userId 用户ID（接收人）
     * @param email 用户邮箱
     * @param realName 用户姓名
     * @param messageTitle 消息标题
     * @param messageType 消息类型
     * @param content 消息内容
     * @param tenantId 租户ID
     * @param noticeId 消息ID
     * @param creatorId 操作人ID（消息发布人）
     */
    void sendMessageNotificationEmail(Long userId, String email, String realName,
                                      String messageTitle, String messageType, String content,
                                      Long tenantId, Long noticeId, Long creatorId);
}
