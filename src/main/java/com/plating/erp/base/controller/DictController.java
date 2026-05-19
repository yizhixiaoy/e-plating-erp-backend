package com.plating.erp.base.controller;

import com.plating.erp.base.service.DictService;
import com.plating.erp.base.vo.DictVo;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.audit.annotation.AuditLog;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理 Controller
 */
@RestController
@RequestMapping("/api/v1/dicts")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    // ==================== 字典类型 ====================

    /**
     * 查询字典类型列表
     */
    @GetMapping("/types")
    @PreAuthorize("@authz.hasPerm('dict:view')")
    public ApiResponse<List<DictVo.DictTypeListVo>> listTypes() {
        return ApiResponse.ok(dictService.listDictTypes());
    }

    /**
     * 新增字典类型
     */
    @PostMapping("/types")
    @PreAuthorize("@authz.hasPerm('dict:add')")
    @AuditLog(module = "字典管理", operateType = "CREATE", bizModule = "dict_type", fieldName = "dictName")
    public ApiResponse<DictVo.DictTypeListVo> createType(@Valid @RequestBody DictVo.DictTypeCreateReq body) {
        return ApiResponse.ok(dictService.createDictType(body));
    }

    /**
     * 编辑字典类型
     */
    @PutMapping("/types/{id}")
    @PreAuthorize("@authz.hasPerm('dict:edit')")
    @AuditLog(module = "字典管理", operateType = "UPDATE", bizModule = "dict_type", fieldName = "dictName")
    public ApiResponse<DictVo.DictTypeListVo> updateType(@PathVariable Long id, @Valid @RequestBody DictVo.DictTypeUpdateReq body) {
        return ApiResponse.ok(dictService.updateDictType(id, body));
    }

    /**
     * 删除字典类型
     */
    @DeleteMapping("/types/{id}")
    @PreAuthorize("@authz.hasPerm('dict:delete')")
    @AuditLog(module = "字典管理", operateType = "DELETE", bizModule = "dict_type", fieldName = "dictName")
    public ApiResponse<CommonResponses.DeleteResponse> deleteType(@PathVariable Long id) {
        boolean deleted = dictService.deleteDictType(id);
        return ApiResponse.ok(new CommonResponses.DeleteResponse(deleted, id));
    }

    // ==================== 字典项 ====================

    /**
     * 查询指定字典类型下的字典项列表
     */
    @GetMapping("/items/{dictType}")
    @PreAuthorize("@authz.hasPerm('dict:view')")
    public ApiResponse<List<DictVo.DictItemListVo>> listItems(@PathVariable String dictType) {
        return ApiResponse.ok(dictService.listDictItems(dictType));
    }

    /**
     * 新增字典项
     */
    @PostMapping("/items")
    @PreAuthorize("@authz.hasPerm('dict:add')")
    @AuditLog(module = "字典管理", operateType = "CREATE", bizModule = "dict_item", fieldName = "dictLabel")
    public ApiResponse<DictVo.DictItemListVo> createItem(@Valid @RequestBody DictVo.DictItemCreateReq body) {
        return ApiResponse.ok(dictService.createDictItem(body));
    }

    /**
     * 编辑字典项
     */
    @PutMapping("/items/{id}")
    @PreAuthorize("@authz.hasPerm('dict:edit')")
    @AuditLog(module = "字典管理", operateType = "UPDATE", bizModule = "dict_item", fieldName = "dictLabel")
    public ApiResponse<DictVo.DictItemListVo> updateItem(@PathVariable Long id, @Valid @RequestBody DictVo.DictItemUpdateReq body) {
        return ApiResponse.ok(dictService.updateDictItem(id, body));
    }

    /**
     * 删除字典项
     */
    @DeleteMapping("/items/{id}")
    @PreAuthorize("@authz.hasPerm('dict:delete')")
    @AuditLog(module = "字典管理", operateType = "DELETE", bizModule = "dict_item", fieldName = "dictLabel")
    public ApiResponse<CommonResponses.DeleteResponse> deleteItem(@PathVariable Long id) {
        boolean deleted = dictService.deleteDictItem(id);
        return ApiResponse.ok(new CommonResponses.DeleteResponse(deleted, id));
    }

    // ==================== 业务查询 ====================

    /**
     * 根据字典类型查询字典数据（供下拉选择使用，无需特定权限）
     */
    @GetMapping("/data/{dictType}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<DictVo.DictDataVo>> getDictData(@PathVariable String dictType) {
        return ApiResponse.ok(dictService.getDictData(dictType));
    }
}