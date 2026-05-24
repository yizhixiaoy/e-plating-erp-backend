package com.plating.erp.message.service.impl;

import com.plating.erp.base.service.DictService;
import com.plating.erp.base.vo.DictVo;
import com.plating.erp.message.entity.EmailRecordEntity;
import com.plating.erp.message.mapper.EmailRecordMapper;
import com.plating.erp.message.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件发送服务实现
 * 支持异步发送、模板替换、发送记录
 */
@Service
public class EmailServiceImpl implements EmailService {
    
    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    
    private final JavaMailSender mailSender;
    private final EmailRecordMapper emailRecordMapper;
    private final DictService dictService;
    
    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;
    
    @Value("${app.mail.from:noreply@example.com}")
    private String mailFrom;
    
    @Value("${app.mail.login-url:http://localhost:5173/login}")
    private String loginUrl;
    
    public EmailServiceImpl(JavaMailSender mailSender, 
                           EmailRecordMapper emailRecordMapper,
                           DictService dictService) {
        this.mailSender = mailSender;
        this.emailRecordMapper = emailRecordMapper;
        this.dictService = dictService;
    }
    
    @Override
    @Async("emailTaskExecutor")
    public void sendEmailAsync(Long tenantId, Long noticeId, String toEmail, String subject, String content) {
        if (!mailEnabled) {
            log.debug("邮件功能未启用,跳过发送: to={}", toEmail);
            return;
        }
        
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("收件人邮箱为空,跳过发送");
            return;
        }
        
        // 创建发送记录
        EmailRecordEntity record = new EmailRecordEntity();
        record.setTenantId(tenantId);
        record.setNoticeId(noticeId);
        record.setReceiverEmail(toEmail);
        record.setSubject(subject);
        record.setContent(content);
        record.setSendStatus(0); // 待发送
        record.setRetryCount(0);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        emailRecordMapper.insert(record);
        
        try {
            // 发送邮件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, false); // 纯文本
            
            mailSender.send(message);
            
            // 更新发送成功
            record.setSendStatus(1);
            record.setSentTime(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            emailRecordMapper.updateById(record);
            
            log.info("邮件发送成功: to={}, subject={}", toEmail, subject);
        } catch (MessagingException e) {
            // 更新发送失败
            record.setSendStatus(2);
            record.setFailReason(e.getMessage());
            record.setUpdatedAt(LocalDateTime.now());
            emailRecordMapper.updateById(record);
            
            log.error("邮件发送失败: to={}, subject={}, error={}", toEmail, subject, e.getMessage(), e);
        }
    }
    
    @Override
    @Async("emailTaskExecutor")
    public void sendNewUserPasswordEmail(Long userId, String email, String realName, 
                                         String username, String plainPassword) {
        if (!mailEnabled) {
            log.debug("邮件功能未启用,跳过新用户密码邮件: to={}", email);
            return;
        }
        
        try {
            // 从字典获取邮件模板
            String subject = getDictValue("email_template", "新用户密码邮件主题");
            String contentTemplate = getDictValue("email_template", "新用户密码邮件内容");
            
            // 模板变量替换
            subject = subject.replace("{tenantName}", "电镀ERP系统");
            contentTemplate = contentTemplate
                    .replace("{realName}", realName != null ? realName : "用户")
                    .replace("{username}", username)
                    .replace("{password}", plainPassword)
                    .replace("{loginUrl}", loginUrl)
                    .replace("{tenantName}", "电镀ERP系统");
            
            sendEmailAsync(0L, null, email, subject, contentTemplate);
            log.info("新用户密码邮件已加入发送队列: userId={}, email={}", userId, email);
        } catch (Exception e) {
            log.error("发送新用户密码邮件异常: userId={}, email={}", userId, email, e);
        }
    }
    
    @Override
    @Async("emailTaskExecutor")
    public void sendTodoNotificationEmail(Long userId, String email, String realName,
                                         String todoTitle, String todoType, Integer priority, String content) {
        if (!mailEnabled) {
            log.debug("邮件功能未启用,跳过待办通知邮件: to={}", email);
            return;
        }
        
        try {
            // 从字典获取邮件模板
            String subject = getDictValue("email_template", "待办通知邮件主题");
            String contentTemplate = getDictValue("email_template", "待办通知邮件内容");
            
            // 模板变量替换
            subject = subject.replace("{todoTitle}", todoTitle);
            contentTemplate = contentTemplate
                    .replace("{realName}", realName != null ? realName : "用户")
                    .replace("{todoTitle}", todoTitle)
                    .replace("{todoType}", todoType != null ? todoType : "任务")
                    .replace("{priority}", priority != null ? getPriorityText(priority) : "中")
                    .replace("{content}", content != null ? content : "");
            
            sendEmailAsync(0L, null, email, subject, contentTemplate);
            log.info("待办通知邮件已加入发送队列: userId={}, email={}", userId, email);
        } catch (Exception e) {
            log.error("发送待办通知邮件异常: userId={}, email={}", userId, email, e);
        }
    }
    
    @Override
    @Async("emailTaskExecutor")
    public void sendMessageNotificationEmail(Long userId, String email, String realName,
                                            String messageTitle, String messageType, String content) {
        if (!mailEnabled) {
            log.debug("邮件功能未启用,跳过消息通知邮件: to={}", email);
            return;
        }
        
        try {
            // 从字典获取邮件模板
            String subject = getDictValue("email_template", "消息通知邮件主题");
            String contentTemplate = getDictValue("email_template", "消息通知邮件内容");
            
            // 模板变量替换
            subject = subject.replace("{messageTitle}", messageTitle);
            contentTemplate = contentTemplate
                    .replace("{realName}", realName != null ? realName : "用户")
                    .replace("{messageTitle}", messageTitle)
                    .replace("{messageType}", messageType != null ? messageType : "通知")
                    .replace("{content}", content != null ? content : "");
            
            sendEmailAsync(0L, null, email, subject, contentTemplate);
            log.info("消息通知邮件已加入发送队列: userId={}, email={}", userId, email);
        } catch (Exception e) {
            log.error("发送消息通知邮件异常: userId={}, email={}", userId, email, e);
        }
    }
    
    /**
     * 从字典获取配置值
     * 通过 dictLabel 匹配标签名,返回 dictValue（模板内容）
     */
    private String getDictValue(String dictType, String dictLabelKey) {
        try {
            List<DictVo.DictDataVo> items = dictService.getDictData(dictType);
            for (DictVo.DictDataVo item : items) {
                if (item.dictLabel().equals(dictLabelKey)) {
                    return item.dictValue();
                }
            }
        } catch (Exception e) {
            log.warn("从字典获取配置失败: dictType={}, dictLabel={}", dictType, dictLabelKey, e);
        }
        return "";
    }
    
    /**
     * 获取优先级文本
     */
    private String getPriorityText(Integer priority) {
        return switch (priority) {
            case 0 -> "低";
            case 1 -> "中";
            case 2 -> "高";
            case 3 -> "紧急";
            default -> "中";
        };
    }
}
