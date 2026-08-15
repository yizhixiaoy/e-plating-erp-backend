package com.plating.erp.iam.vo;

/**
 * 部门下拉选项 VO
 * 
 * 用于用户表单中选择部门
 */
public record DeptOptionsVo(
        Long id,
        String deptName
) {
}
