package com.plating.erp.todo.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Todo 模块 VO 集合。
 */
public final class TodoVo {

    private TodoVo() {}

    /** 创建待办请求（业务模块/管理员调用） */
    public record CreateReq(
            @NotNull(message = "处理人不能为空")
            Long assigneeId,
            String todoType,
            Integer priority,
            @NotBlank(message = "标题不能为空")
            @Size(max = 200, message = "标题最长 200 字符")
            String title,
            String content,
            String bizModule,
            String bizRefId,
            String bizRefUrl,
            LocalDateTime deadline
    ) {}

    /** 处理动作请求 */
    public record HandleReq(
            @NotBlank(message = "动作不能为空")
            String action,
            String remark,
            /** 仅 TRANSFER 时使用 */
            Long targetUserId
    ) {}

    /** 列表查询过滤 */
    public record QueryReq(
            String todoType,
            Integer status,
            Integer priority,
            String keyword
    ) {}

    /** 视图（前端列表 / 详情共用） */
    public record View(
            Long id,
            Long tenantId,
            Long assigneeId,
            Long creatorId,
            String todoType,
            Integer priority,
            String title,
            String content,
            String bizModule,
            String bizRefId,
            String bizRefUrl,
            Integer status,
            String handleAction,
            String handleRemark,
            LocalDateTime handledAt,
            LocalDateTime deadline,
            Integer readStatus,
            LocalDateTime readTime,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    /** 统计 */
    public record Stats(
            long pending,
            long done,
            long ignored,
            long transferred,
            long overdue,
            long todayNew
    ) {}
}
