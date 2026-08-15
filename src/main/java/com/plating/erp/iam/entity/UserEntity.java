package com.plating.erp.iam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 
 * 包含用户基本信息、登录信息、员工信息、审计信息等
 */
@Data
@TableName("sys_user")
public class UserEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String username;
    private String passwordHash;
    private String realName;
    private String avatarUrl;
    private Long deptId;
    private String position;          // 岗位/职位
    private Long leaderUserId;        // 直属领导用户ID
    private String phone;
    private String officePhone;       // 办公电话
    private String email;
    private LocalDate joinDate;       // 入职日期
    private Integer employeeStatus;   // 员工状态:0在职 1离职 2试用期
    private Integer userType;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private Integer loginCount;
    private String wechatOpenid;
    private String dingtalkUserid;
    
    // 关联字段（非数据库字段）
    @TableField(exist = false)
    private String tenantName;        // 租户名称（联表查询填充）
    @TableField(exist = false)
    private String shortName;
    
    // 审计字段
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
