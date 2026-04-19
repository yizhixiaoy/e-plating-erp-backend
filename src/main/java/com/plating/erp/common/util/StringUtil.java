package com.plating.erp.common.util;

/**
 * 字符串工具类
 * 
 * 提供字符串处理的通用方法
 */
public final class StringUtil {
    
    private StringUtil() {
        // 防止实例化
    }
    
    /**
     * 将空字符串转换为 null
     * 
     * 使用场景：
     * - 前端表单清空字段时传空字符串 ""
     * - 后端统一转换为 null 存入数据库
     * 
     * @param str 原始字符串
     * @return 如果为空字符串则返回 null，否则返回原字符串
     */
    public static String blankToNull(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        return str;
    }
    
    /**
     * 将 null 转换为空字符串
     * 
     * 使用场景：
     * - 数据库字段为 null 时，前端显示为空字符串
     * 
     * @param str 原始字符串
     * @return 如果为 null 则返回空字符串，否则返回原字符串
     */
    public static String nullToBlank(String str) {
        return str == null ? "" : str;
    }
}
