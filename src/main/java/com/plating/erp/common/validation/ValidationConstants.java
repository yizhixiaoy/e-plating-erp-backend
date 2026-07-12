package com.plating.erp.common.validation;

/**
 * 统一校验常量
 * 
 * 与前端 validation.ts 和数据库约束保持一致
 *
 */
public final class ValidationConstants {
    
    private ValidationConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
    
    // ==================== 长度常量 ====================
    
    /** 账号最小长度 */
    public static final int USERNAME_MIN_LENGTH = 4;
    /** 账号最大长度 */
    public static final int USERNAME_MAX_LENGTH = 12;
    
    /** 密码最小长度 */
    public static final int PASSWORD_MIN_LENGTH = 8;
    /** 密码最大长度 */
    public static final int PASSWORD_MAX_LENGTH = 20;
    
    /** 姓名最小长度 */
    public static final int REAL_NAME_MIN_LENGTH = 2;
    /** 姓名最大长度 */
    public static final int REAL_NAME_MAX_LENGTH = 30;
    
    /** 租户名称最小长度 */
    public static final int TENANT_NAME_MIN_LENGTH = 2;
    /** 租户名称最大长度 */
    public static final int TENANT_NAME_MAX_LENGTH = 64;
    
    /** 租户简称最小长度 */
    public static final int SHORT_CODE_MIN_LENGTH = 2;
    /** 租户简称最大长度 */
    public static final int SHORT_CODE_MAX_LENGTH = 16;
    
    /** 联系人最大长度 */
    public static final int CONTACT_NAME_MAX_LENGTH = 32;
    
    /** 角色名称最小长度 */
    public static final int ROLE_NAME_MIN_LENGTH = 2;
    /** 角色名称最大长度 */
    public static final int ROLE_NAME_MAX_LENGTH = 32;
    
    /** 角色权限字符最小长度 */
    public static final int ROLE_KEY_MIN_LENGTH = 1;
    /** 角色权限字符最大长度 */
    public static final int ROLE_KEY_MAX_LENGTH = 64;

    /** 部门名称最大长度 */
    public static final int DEPT_NAME_MAX_LENGTH = 64;

    /** 岗位名称最大长度 */
    public static final int POSITION_NAME_MAX_LENGTH = 50;
    
    // ==================== 正则表达式常量 ====================
    
    /** 手机号正则：11位，1开头，第二位3-9 */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    /** 手机号正则消息 */
    public static final String PHONE_MESSAGE = "请输入正确的11位手机号";
    
    /** 邮箱正则 */
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    /** 邮箱正则消息 */
    public static final String EMAIL_MESSAGE = "请输入正确的邮箱地址";
    
    /** 账号正则：字母、数字和特殊字符(:@._-) */
    public static final String USERNAME_REGEX = "^[a-zA-Z0-9:@._-]+$";
    /** 账号正则消息 */
    public static final String USERNAME_MESSAGE = "账号只能包含字母、数字和特殊字符(:@._-)";
    
    /** 密码正则：包含字母和数字 */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$";
    /** 密码正则消息 */
    public static final String PASSWORD_MESSAGE = "密码必须包含字母和数字";
    
    /** 租户简称正则：字母和数字 */
    public static final String SHORT_CODE_REGEX = "^[a-zA-Z0-9]+$";
    /** 租户简称正则消息 */
    public static final String SHORT_CODE_MESSAGE = "简称只能包含字母和数字";
    
    /** 角色权限字符正则：字母、数字和下划线 */
    public static final String ROLE_KEY_REGEX = "^[a-zA-Z0-9_]+$";
    /** 角色权限字符正则消息 */
    public static final String ROLE_KEY_MESSAGE = "权限字符只能包含字母、数字和下划线";
    
    /** URL正则 */
    public static final String URL_REGEX = "^https?:\\/\\/.*";
    /** URL正则消息 */
    public static final String URL_MESSAGE = "请输入有效的URL地址";
    
    /** 域名最大长度 */
    public static final int DOMAIN_MAX_LENGTH = 128;
    
    /** 域名正则：支持子域名和主域名 */
    public static final String DOMAIN_REGEX = "^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*$";
    /** 域名正则消息 */
    public static final String DOMAIN_MESSAGE = "域名格式不正确，示例：company.example.com";
}
