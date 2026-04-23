package com.plating.erp.iam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单实体类
 * 支持三种类型：M=目录 C=菜单 F=按钮
 */
@Data
@TableName("sys_menu")
public class MenuEntity {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /** 父菜单ID */
    private Long parentId;
    
    /** 菜单名称 */
    private String menuName;
    
    /** 菜单类型 M=目录 C=菜单 F=按钮 */
    private String menuType;
    
    /** 路由路径 */
    private String path;
    
    /** 菜单图标 */
    private String icon;
    
    /** 权限标识 */
    private String perms;
    
    /** 排序 */
    private Integer sortNo;
    
    /** 状态 0=正常 1=停用 */
    private Integer status;
    
    /** 是否可见 0=隐藏 1=显示 */
    private Integer visible;
    
    /** 组件路径 */
    private String component;
    
    /** 租户ID(0=平台级) */
    private Long tenantId;
    
    /** 创建人 */
    private Long createdBy;
    
    /** 创建时间 */
    private LocalDateTime createdAt;
    
    /** 更新人 */
    private Long updatedBy;
    
    /** 更新时间 */
    private LocalDateTime updatedAt;
    
    /** 删除标记 */
    private Integer deleted;
}
