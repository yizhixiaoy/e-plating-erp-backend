package com.plating.erp.iam.controller;

import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.iam.entity.DeptEntity;
import com.plating.erp.iam.entity.PositionEntity;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.DeptMapper;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.iam.service.PositionService;
import com.plating.erp.iam.vo.PositionVo;
import com.plating.erp.platform.entity.TenantEntity;
import com.plating.erp.platform.mapper.TenantMapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 岗位控制器
 * 提供岗位 CRUD、下拉选项等接口
 */
@RestController
@RequestMapping("/api/v1/positions")
public class PositionController {

    private final PositionService positionService;
    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final DeptMapper deptMapper;

    public PositionController(PositionService positionService, UserMapper userMapper, TenantMapper tenantMapper, DeptMapper deptMapper) {
        this.positionService = positionService;
        this.userMapper = userMapper;
        this.tenantMapper = tenantMapper;
        this.deptMapper = deptMapper;
    }

    /**
     * 分页查询岗位列表
     */
    @GetMapping
    @PreAuthorize("@authz.hasPerm('position:view')")
    public ApiResponse<PageResult<PositionVo.PositionListVo>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                    @RequestParam(defaultValue = "100") Integer pageSize,
                                                                    @RequestParam(required = false) String keyword,
                                                                    @RequestParam(required = false) Long filterTenantId,
                                                                    @RequestParam(required = false) Long filterDeptId) {
        var me = SecurityUtils.currentUser();
        var page = positionService.page(pageNum, pageSize, me.tenantId(), me.isSystem(), filterTenantId, filterDeptId, keyword);

        Map<Long, String> userNameMap = buildUserNameMap(page.records());
        Map<Long, String> tenantNameMap = buildTenantNameMap(page.records());
        Map<Long, String> deptNameMap = buildDeptNameMap(page.records());

        List<PositionVo.PositionListVo> voList = page.records().stream().map(p -> new PositionVo.PositionListVo(
                p.getId(),
                p.getTenantId(),
                tenantNameMap.getOrDefault(p.getTenantId(), null),
                p.getDeptId(),
                deptNameMap.getOrDefault(p.getDeptId(), null),
                p.getPositionName(),
                p.getSortNo(),
                p.getStatus(),
                p.getCreatedBy(),
                userNameMap.getOrDefault(p.getCreatedBy(), null),
                p.getCreatedAt(),
                p.getUpdatedBy(),
                userNameMap.getOrDefault(p.getUpdatedBy(), null),
                p.getUpdatedAt()
        )).toList();

        return ApiResponse.ok(new PageResult<>(voList, page.total()));
    }

    /**
     * 获取所有岗位（下拉选项用，支持按公司和部门过滤）
     */
    @GetMapping("/options")
    @PreAuthorize("@authz.hasPerm('user:add') or @authz.hasPerm('user:edit') or @authz.hasPerm('position:view')")
    public ApiResponse<List<String>> options(@RequestParam(required = false) Long tenantId,
                                              @RequestParam(required = false) Long deptId) {
        var me = SecurityUtils.currentUser();
        Long queryTenantId = me.isSystem() ? tenantId : me.tenantId();
        if (queryTenantId == null) {
            return ApiResponse.ok(List.of());
        }

        List<PositionEntity> positions = positionService.listAll(queryTenantId, deptId);
        List<String> names = positions.stream().map(PositionEntity::getPositionName).toList();
        return ApiResponse.ok(names);
    }

    /**
     * 新增岗位
     */
    @PostMapping
    @PreAuthorize("@authz.hasPerm('position:add')")
    @AuditLog(module = "岗位管理", operateType = "CREATE", bizModule = "position", fieldName = "positionName")
    public ApiResponse<PositionEntity> create(@Valid @RequestBody PositionVo.PositionCreateReq body) {
        var me = SecurityUtils.currentUser();
        PositionEntity entity = new PositionEntity();
        // 平台管理员可指定租户，租户用户只能创建自己公司的
        entity.setTenantId(me.isSystem() && body.tenantId() != null ? body.tenantId() : me.tenantId());
        entity.setDeptId(body.deptId() == null ? 0L : body.deptId());
        entity.setPositionName(body.positionName());
        entity.setSortNo(body.sortNo() == null ? 0 : body.sortNo());
        entity.setStatus(body.status() == null ? 0 : body.status());
        entity.setCreatedBy(me.userId());
        entity.setUpdatedBy(me.userId());
        PositionEntity saved = positionService.save(entity);
        return ApiResponse.ok(saved);
    }

    /**
     * 编辑岗位
     */
    @PutMapping("/{positionId}")
    @PreAuthorize("@authz.hasPerm('position:edit')")
    @AuditLog(module = "岗位管理", operateType = "UPDATE", bizModule = "position", fieldName = "positionName")
    public ApiResponse<PositionEntity> update(@PathVariable Long positionId, @Valid @RequestBody PositionVo.PositionUpdateReq body) {
        PositionEntity existing = positionService.getById(positionId);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "岗位不存在");
        }
        assertPositionTenant(existing);

        var me = SecurityUtils.currentUser();
        PositionEntity entity = new PositionEntity();
        entity.setId(positionId);
        entity.setTenantId(existing.getTenantId());
        entity.setDeptId(body.deptId() == null ? existing.getDeptId() : body.deptId());
        entity.setPositionName(body.positionName() == null ? existing.getPositionName() : body.positionName());
        entity.setSortNo(body.sortNo() == null ? existing.getSortNo() : body.sortNo());
        entity.setStatus(body.status() == null ? existing.getStatus() : body.status());
        entity.setUpdatedBy(me.userId());
        positionService.updateById(entity);
        PositionEntity updated = positionService.getById(positionId);
        return ApiResponse.ok(updated);
    }

    /**
     * 删除岗位
     */
    @DeleteMapping("/{positionId}")
    @PreAuthorize("@authz.hasPerm('position:delete')")
    @AuditLog(module = "岗位管理", operateType = "DELETE", bizModule = "position", fieldName = "positionName")
    public ApiResponse<CommonResponses.DeleteResponse> delete(@PathVariable Long positionId) {
        var me = SecurityUtils.currentUser();
        PositionEntity existing = positionService.getById(positionId);
        if (existing == null) {
            return ApiResponse.ok(new CommonResponses.DeleteResponse(false, positionId));
        }
        assertPositionTenant(existing);
        boolean deleted = positionService.delete(positionId, me.tenantId(), me.isSystem());
        return ApiResponse.ok(new CommonResponses.DeleteResponse(deleted, positionId));
    }

    private void assertPositionTenant(PositionEntity position) {
        var me = SecurityUtils.currentUser();
        if (me.isSystem()) {
            return;
        }
        if (position.getTenantId() == null || !position.getTenantId().equals(me.tenantId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该岗位");
        }
    }

    private Map<Long, String> buildUserNameMap(List<PositionEntity> positions) {
        List<Long> userIds = positions.stream()
                .flatMap(p -> java.util.stream.Stream.of(p.getCreatedBy(), p.getUpdatedBy()))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<UserEntity> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getRealName, (a, b) -> a));
    }

    private Map<Long, String> buildTenantNameMap(List<PositionEntity> positions) {
        List<Long> tenantIds = positions.stream()
                .map(PositionEntity::getTenantId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (tenantIds.isEmpty()) {
            return Map.of();
        }
        List<TenantEntity> tenants = tenantMapper.selectBatchIds(tenantIds);
        return tenants.stream().collect(Collectors.toMap(TenantEntity::getId, TenantEntity::getTenantName, (a, b) -> a));
    }

    private Map<Long, String> buildDeptNameMap(List<PositionEntity> positions) {
        List<Long> deptIds = positions.stream()
                .map(PositionEntity::getDeptId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        List<DeptEntity> depts = deptMapper.selectBatchIds(deptIds);
        return depts.stream().collect(Collectors.toMap(DeptEntity::getId, DeptEntity::getDeptName, (a, b) -> a));
    }
}
