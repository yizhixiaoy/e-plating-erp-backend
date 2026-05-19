package com.plating.erp.base.service;

import com.plating.erp.base.vo.DictVo;

import java.util.List;

/**
 * 字典管理 Service
 * 支持多租户数据隔离：系统字典(tenantId=0) + 租户自定义字典
 */
public interface DictService {

    // ==================== 字典类型 ====================

    /**
     * 查询字典类型列表（当前租户可见的所有类型：系统类型 + 自定义类型）
     */
    List<DictVo.DictTypeListVo> listDictTypes();

    /**
     * 新增字典类型
     */
    DictVo.DictTypeListVo createDictType(DictVo.DictTypeCreateReq req);

    /**
     * 编辑字典类型
     */
    DictVo.DictTypeListVo updateDictType(Long id, DictVo.DictTypeUpdateReq req);

    /**
     * 删除字典类型（同时删除关联的字典项）
     */
    boolean deleteDictType(Long id);

    // ==================== 字典项 ====================

    /**
     * 查询指定字典类型下的字典项列表
     */
    List<DictVo.DictItemListVo> listDictItems(String dictType);

    /**
     * 新增字典项
     */
    DictVo.DictItemListVo createDictItem(DictVo.DictItemCreateReq req);

    /**
     * 编辑字典项
     */
    DictVo.DictItemListVo updateDictItem(Long id, DictVo.DictItemUpdateReq req);

    /**
     * 删除字典项
     */
    boolean deleteDictItem(Long id);

    // ==================== 业务查询 ====================

    /**
     * 根据字典类型查询字典数据（供下拉选择使用）
     * 返回正常状态的字典项
     */
    List<DictVo.DictDataVo> getDictData(String dictType);
}