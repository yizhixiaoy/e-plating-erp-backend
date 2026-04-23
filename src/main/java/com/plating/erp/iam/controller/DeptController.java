package com.plating.erp.iam.controller;

import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.iam.entity.DeptEntity;
import com.plating.erp.iam.mapper.DeptMapper;
import com.plating.erp.iam.vo.DeptOptionsVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门控制器
 * 提供部门下拉选项等查询接口
 */
@RestController
@RequestMapping("/api/v1/depts")
public class DeptController {

    private final DeptMapper deptMapper;

    public DeptController(DeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    /**
     * 获取部门下拉选项
     * 
     * @param tenantId 租户ID（平台管理员查询指定租户的部门，租户用户自动使用自己租户ID）
     * @return 部门选项列表
     */
    @GetMapping("/options")
    @PreAuthorize("@authz.hasPerm('user:add') or @authz.hasPerm('user:edit')")
    public ApiResponse<List<DeptOptionsVo>> options(@RequestParam(required = false) Long tenantId) {
        var me = SecurityUtils.currentUser();
        Long queryTenantId = me.isSystem() ? tenantId : me.tenantId();
        if (queryTenantId == null) {
            return ApiResponse.ok(List.of());
        }

        List<DeptEntity> depts = deptMapper.selectList(
                new LambdaQueryWrapper<DeptEntity>()
                        .eq(DeptEntity::getTenantId, queryTenantId)
                        .eq(DeptEntity::getStatus, 0)
                        .orderByAsc(DeptEntity::getSortNo)
        );

        List<DeptOptionsVo> voList = depts.stream()
                .map(d -> new DeptOptionsVo(d.getId(), d.getDeptName()))
                .toList();
        return ApiResponse.ok(voList);
    }
}
