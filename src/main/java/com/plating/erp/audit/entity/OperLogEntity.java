package com.plating.erp.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class OperLogEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String moduleTitle;
    private String operateType;
    private Long userId;
    private String userName;
    private String requestUrl;
    private String requestMethod;
    private String methodName;
    private String requestParams;
    private String responseResult;
    private Integer status;
    private String errorMsg;
    private Integer executeTime;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
