package com.plating.erp.audit.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_oper_log")
public class OperLogEntity {
    @TableId
    private Long id;
    private Long tenantId;
    private String moduleTitle;
    private String operateType;
    private Long userId;
    private String requestParams;
    private Integer status;
}
