package com.plating.erp.iam.controller;

import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.iam.entity.DeptEntity;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.iam.service.DeptService;
import com.plating.erp.iam.vo.DeptOptionsVo;
import com.plating.erp.iam.vo.DeptVo;
import com.plating.erp.platform.entity.TenantEntity;
import com.plating.erp.platform.mapper.TenantMapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门控制器
 * 提供部门 CRUD、下拉选项等接口
 */
@RestController
@RequestMapping("/api/v1/depts")
public class DeptController {

    private final DeptService deptService;
    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;

    public DeptController(DeptService deptService, UserMapper userMapper, TenantMapper tenantMapper) {
        this.deptService = deptService;
        this.userMapper = userMapper;
        this.tenantMapper = tenantMapper;
    }

    /**
     * 分页查询部门列表
     */
    @GetMapping
    @PreAuthorize("@authz.hasPerm('dept:view')")
    public ApiResponse<PageResult<DeptVo.DeptListVo>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "100") Integer pageSize,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Long filterTenantId) {
        var me = SecurityUtils.currentUser();
        var page = deptService.page(pageNum, pageSize, me.tenantId(), me.isSystem(), filterTenantId, keyword);

        // 批量获取用户ID集合，一次查询拿到所有用户信息
        List<Long> userIds = page.records().stream()
                .flatMap(d -> java.util.stream.Stream.of(d.getLeaderUserId(), d.getCreatedBy(), d.getUpdatedBy()))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        Map<Long, UserEntity> userMap = userIds.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(UserEntity::getId, u -> u, (a, b) -> a));

        Map<Long, String> tenantNameMap = buildTenantNameMap(page.records());

        List<DeptVo.DeptListVo> voList = page.records().stream().map(d -> {
            UserEntity leader = userMap.get(d.getLeaderUserId());
            UserEntity creator = userMap.get(d.getCreatedBy());
            UserEntity updater = userMap.get(d.getUpdatedBy());
            return new DeptVo.DeptListVo(
                    d.getId(),
                    d.getTenantId(),
                    tenantNameMap.getOrDefault(d.getTenantId(), null),
                    d.getParentId(),
                    d.getDeptName(),
                    d.getLeaderUserId(),
                    leader != null ? leader.getRealName() : null,
                    leader != null ? leader.getUsername() : null,
                    d.getSortNo(),
                    d.getStatus(),
                    d.getCreatedBy(),
                    creator != null ? creator.getRealName() : null,
                    d.getCreatedAt(),
                    d.getUpdatedBy(),
                    updater != null ? updater.getRealName() : null,
                    d.getUpdatedAt()
            );
        }).toList();

        return ApiResponse.ok(new PageResult<>(voList, page.total()));
    }

    /**
     * 获取所有部门（树形/下拉选项用）
     */
    @GetMapping("/options")
    @PreAuthorize("@authz.hasPerm('user:add') or @authz.hasPerm('user:edit') or @authz.hasPerm('dept:view') or @authz.hasPerm('position:add') or @authz.hasPerm('position:edit') or @authz.hasPerm('message:add')")
    public ApiResponse<List<DeptOptionsVo>> options(@RequestParam(required = false) Long tenantId) {
        var me = SecurityUtils.currentUser();
        Long queryTenantId = me.isSystem() ? tenantId : me.tenantId();
        if (queryTenantId == null) {
            return ApiResponse.ok(List.of());
        }

        List<DeptEntity> depts = deptService.listAll(queryTenantId);

        List<DeptOptionsVo> voList = depts.stream()
                .map(d -> new DeptOptionsVo(d.getId(), d.getDeptName()))
                .toList();
        return ApiResponse.ok(voList);
    }

    /**
     * 新增部门
     */
    @PostMapping
    @PreAuthorize("@authz.hasPerm('dept:add')")
    @AuditLog(module = "部门管理", operateType = "CREATE", bizModule = "dept", fieldName = "deptName")
    public ApiResponse<DeptEntity> create(@Valid @RequestBody DeptVo.DeptCreateReq body) {
        var me = SecurityUtils.currentUser();
        DeptEntity entity = new DeptEntity();
        // 平台管理员可指定租户，租户用户只能创建自己公司的
        entity.setTenantId(me.isSystem() && body.tenantId() != null ? body.tenantId() : me.tenantId());
        entity.setDeptName(body.deptName());
        entity.setParentId(body.parentId() == null ? 0L : body.parentId());
        entity.setLeaderUserId(body.leaderUserId());
        entity.setSortNo(body.sortNo() == null ? 0 : body.sortNo());
        entity.setStatus(body.status() == null ? 0 : body.status());
        entity.setCreatedBy(me.userId());
        entity.setUpdatedBy(me.userId());
        DeptEntity saved = deptService.save(entity);
        return ApiResponse.ok(saved);
    }

    /**
     * 编辑部门
     */
    @PutMapping("/{deptId}")
    @PreAuthorize("@authz.hasPerm('dept:edit')")
    @AuditLog(module = "部门管理", operateType = "UPDATE", bizModule = "dept", fieldName = "deptName")
    public ApiResponse<DeptEntity> update(@PathVariable Long deptId, @Valid @RequestBody DeptVo.DeptUpdateReq body) {
        DeptEntity existing = deptService.getById(deptId);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "部门不存在");
        }
        assertDeptTenant(existing);

        var me = SecurityUtils.currentUser();
        DeptEntity entity = new DeptEntity();
        entity.setId(deptId);
        entity.setTenantId(existing.getTenantId());
        entity.setDeptName(body.deptName() == null ? existing.getDeptName() : body.deptName());
        entity.setParentId(body.parentId() == null ? existing.getParentId() : body.parentId());
        entity.setLeaderUserId(body.leaderUserId() != null ? body.leaderUserId() : existing.getLeaderUserId());
        entity.setSortNo(body.sortNo() == null ? existing.getSortNo() : body.sortNo());
        entity.setStatus(body.status() == null ? existing.getStatus() : body.status());
        entity.setUpdatedBy(me.userId());
        deptService.updateById(entity);
        DeptEntity updated = deptService.getById(deptId);
        return ApiResponse.ok(updated);
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{deptId}")
    @PreAuthorize("@authz.hasPerm('dept:delete')")
    @AuditLog(module = "部门管理", operateType = "DELETE", bizModule = "dept", fieldName = "deptName")
    public ApiResponse<CommonResponses.DeleteResponse> delete(@PathVariable Long deptId) {
        var me = SecurityUtils.currentUser();
        DeptEntity existing = deptService.getById(deptId);
        if (existing == null) {
            return ApiResponse.ok(new CommonResponses.DeleteResponse(false, deptId));
        }
        assertDeptTenant(existing);
        boolean deleted = deptService.delete(deptId, me.tenantId(), me.isSystem());
        return ApiResponse.ok(new CommonResponses.DeleteResponse(deleted, deptId));
    }

    /**
     * 验证部门所属租户
     */
    private void assertDeptTenant(DeptEntity dept) {
        var me = SecurityUtils.currentUser();
        if (me.isSystem()) {
            return;
        }
        if (dept.getTenantId() == null || !dept.getTenantId().equals(me.tenantId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该部门");
        }
    }

    /**
     * 批量获取租户名映射
     */
    private Map<Long, String> buildTenantNameMap(List<DeptEntity> depts) {
        List<Long> tenantIds = depts.stream()
                .map(DeptEntity::getTenantId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (tenantIds.isEmpty()) {
            return Map.of();
        }
        List<TenantEntity> tenants = tenantMapper.selectBatchIds(tenantIds);
        return tenants.stream().collect(Collectors.toMap(TenantEntity::getId, TenantEntity::getTenantName, (a, b) -> a));
    }
}
