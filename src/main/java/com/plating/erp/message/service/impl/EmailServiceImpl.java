package com.plating.erp.message.service.impl;

import com.plating.erp.base.service.DictService;
import com.plating.erp.base.vo.DictVo;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.message.entity.EmailRecordEntity;
import com.plating.erp.message.mapper.EmailRecordMapper;
import com.plating.erp.message.service.EmailService;
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
    private final UserMapper userMapper;
    
    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;
    
    @Value("${app.mail.from:noreply@example.com}")
    private String mailFrom;
    
    @Value("${app.mail.login-url:http://localhost:5173/login}")
    private String loginUrl;
    
    public EmailServiceImpl(JavaMailSender mailSender, 
                           EmailRecordMapper emailRecordMapper,
                           DictService dictService,
                           UserMapper userMapper) {
        this.mailSender = mailSender;
        this.emailRecordMapper = emailRecordMapper;
        this.dictService = dictService;
        this.userMapper = userMapper;
    }
    
    @Override
    @Async("emailTaskExecutor")
    public void sendEmailAsync(Long tenantId, Long userId, Long noticeId, String senderEmail, String toEmail, String subject, String content, Long operatorId) {
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
        record.setReceiverUserId(userId);
        record.setNoticeId(noticeId);
        record.setSenderEmail(senderEmail);
        record.setReceiverEmail(toEmail);
        record.setSubject(subject);
        record.setContent(content);
        record.setOperatorId(operatorId);
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
        } catch (Exception e) {
            // 更新发送失败
            record.setSendStatus(2);
            record.setFailReason(e.getClass().getSimpleName() + ": " + e.getMessage());
            record.setUpdatedAt(LocalDateTime.now());
            emailRecordMapper.updateById(record);
            
            log.error("邮件发送失败: to={}, subject={}, error={}", toEmail, subject, e.getMessage(), e);
        }
    }
    
    @Override
    public void sendNewUserPasswordEmail(Long userId, String email, String realName, 
                                         String username, String plainPassword, Long tenantId, Long creatorId) {
        if (!mailEnabled) {
            log.debug("邮件功能未启用,跳过新用户密码邮件: to={}", email);
            return;
        }
        
        try {
            // 从字典获取邮件模板
            String subject = getDictValue("email_template", "新用户密码邮件主题");
            String contentTemplate = getDictValue("email_template", "新用户密码邮件内容");
            
            // 模板变量替换
            String tenantName = getDictValue("email_template", "租户名称");
            if (tenantName.isBlank()) tenantName = "电镀ERP系统";
            subject = subject.replace("{tenantName}", tenantName);
            contentTemplate = contentTemplate
                    .replace("{realName}", realName != null ? realName : "用户")
                    .replace("{username}", username)
                    .replace("{password}", plainPassword)
                    .replace("{loginUrl}", loginUrl)
                    .replace("{tenantName}", tenantName);
            
            String senderEmail = resolveSenderEmail(creatorId);
            sendEmailAsync(tenantId != null ? tenantId : 0L, userId, null, senderEmail, email, subject, contentTemplate, creatorId);
            log.info("新用户密码邮件已加入发送队列: userId={}, email={}", userId, email);
        } catch (Exception e) {
            log.error("发送新用户密码邮件异常: userId={}, email={}", userId, email, e);
        }
    }

    @Override
    public void sendResetPasswordEmail(Long userId, String email, String realName,
                                       String username, String plainPassword, Long tenantId, Long operatorId) {
        if (!mailEnabled) {
            log.debug("邮件功能未启用,跳过重置密码邮件: to={}", email);
            return;
        }

        try {
            // 从字典获取重置密码专用邮件模板（区别于新用户注册模板）
            String subject = getDictValue("email_template", "重置密码邮件主题");
            String contentTemplate = getDictValue("email_template", "重置密码邮件内容");

            // 模板变量替换
            String tenantName = getDictValue("email_template", "租户名称");
            if (tenantName.isBlank()) tenantName = "电镀ERP系统";
            subject = subject.replace("{tenantName}", tenantName);
            contentTemplate = contentTemplate
                    .replace("{realName}", realName != null ? realName : "用户")
                    .replace("{username}", username)
                    .replace("{password}", plainPassword)
                    .replace("{loginUrl}", loginUrl)
                    .replace("{tenantName}", tenantName);

            String senderEmail = resolveSenderEmail(operatorId);
            sendEmailAsync(tenantId != null ? tenantId : 0L, userId, null, senderEmail, email, subject, contentTemplate, operatorId);
            log.info("重置密码邮件已加入发送队列: userId={}, email={}", userId, email);
        } catch (Exception e) {
            log.error("发送重置密码邮件异常: userId={}, email={}", userId, email, e);
        }
    }

    @Override
    public void sendTodoNotificationEmail(Long userId, String email, String realName,
                                         String todoTitle, String todoType, Integer priority, String content,
                                         Long tenantId, Long relatedId, Long creatorId) {
        if (!mailEnabled) {
            log.debug("邮件功能未启用,跳过待办通知邮件: to={}", email);
            return;
        }
        
        try {
            // 从字典获取邮件模板
            String subject = getDictValue("email_template", "待办通知邮件主题");
            String contentTemplate = getDictValue("email_template", "待办通知邮件内容");
            
            // 用字典标签替换原始类型的值（如 TASK → 任务）
            String typeLabel = getDictLabel("todo_type", todoType);
            
            // 模板变量替换
            subject = subject.replace("{todoTitle}", todoTitle);
            contentTemplate = contentTemplate
                    .replace("{realName}", realName != null ? realName : "用户")
                    .replace("{todoTitle}", todoTitle)
                    .replace("{todoType}", typeLabel != null ? typeLabel : (todoType != null ? todoType : "任务"))
                    .replace("{priority}", priority != null ? getPriorityText(priority) : "中")
                    .replace("{content}", content != null ? content : "");
            
            String senderEmail = resolveSenderEmail(creatorId);
            sendEmailAsync(tenantId != null ? tenantId : 0L, userId, relatedId, senderEmail, email, subject, contentTemplate, creatorId);
            log.info("待办通知邮件已加入发送队列: userId={}, email={}", userId, email);
        } catch (Exception e) {
            log.error("发送待办通知邮件异常: userId={}, email={}", userId, email, e);
        }
    }
    
    @Override
    public void sendMessageNotificationEmail(Long userId, String email, String realName,
                                            String messageTitle, String messageType, String content,
                                            Long tenantId, Long noticeId, Long creatorId) {
        if (!mailEnabled) {
            log.debug("邮件功能未启用,跳过消息通知邮件: to={}", email);
            return;
        }
        
        try {
            // 从字典获取邮件模板
            String subject = getDictValue("email_template", "消息通知邮件主题");
            String contentTemplate = getDictValue("email_template", "消息通知邮件内容");
            
            // 用字典标签替换原始类型的值（如 system → 系统通知）
            String typeLabel = getDictLabel("notice_type", messageType);
            
            // 模板变量替换
            subject = subject.replace("{messageTitle}", messageTitle);
            contentTemplate = contentTemplate
                    .replace("{realName}", realName != null ? realName : "用户")
                    .replace("{messageTitle}", messageTitle)
                    .replace("{messageType}", typeLabel != null ? typeLabel : (messageType != null ? messageType : "通知"))
                    .replace("{content}", content != null ? content : "");
            
            String senderEmail = resolveSenderEmail(creatorId);
            sendEmailAsync(tenantId != null ? tenantId : 0L, userId, noticeId, senderEmail, email, subject, contentTemplate, creatorId);
            log.info("消息通知邮件已加入发送队列: userId={}, email={}", userId, email);
        } catch (Exception e) {
            log.error("发送消息通知邮件异常: userId={}, email={}", userId, email, e);
        }
    }
    
    /**
     * 根据操作人ID解析发件人标识
     * 格式：姓名 <email>，操作人无邮箱时仅记录姓名
     */
    private String resolveSenderEmail(Long creatorId) {
        if (creatorId == null) return null;
        try {
            UserEntity sender = userMapper.selectById(creatorId);
            if (sender != null) {
                if (sender.getEmail() != null && !sender.getEmail().isBlank()) {
                    return sender.getRealName() + " <" + sender.getEmail() + ">";
                }
                return sender.getRealName();
            }
        } catch (Exception e) {
            log.warn("解析发件人信息失败: creatorId={}", creatorId, e);
        }
        return null;
    }
    
    /**
     * 从字典获取字典标签（根据 dictValue 查找 dictLabel）
     * 用于将类型值（如 TASK）转换为显示标签（如 任务）
     */
    private String getDictLabel(String dictType, String dictValue) {
        if (dictValue == null || dictValue.isBlank()) return null;
        try {
            List<DictVo.DictDataVo> items = dictService.getDictData(dictType);
            for (DictVo.DictDataVo item : items) {
                if (item.dictValue().equalsIgnoreCase(dictValue)) {
                    return item.dictLabel();
                }
            }
        } catch (Exception e) {
            log.warn("从字典获取标签失败: dictType={}, dictValue={}", dictType, dictValue, e);
        }
        return null;
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
