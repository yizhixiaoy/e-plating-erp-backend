package com.plating.erp.todo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_todo_handle_log")
public class TodoHandleLogEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long todoId;
    private Long operatorId;
    /** AGREE/REJECT/TRANSFER/COMPLETE/IGNORE/READ */
    private String action;
    private String remark;
    private Long targetUser;
    private LocalDateTime createdAt;
}
