package com.plating.erp.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.plating.erp.base.entity.DictItemEntity;
import com.plating.erp.base.entity.DictTypeEntity;
import com.plating.erp.base.mapper.DictItemMapper;
import com.plating.erp.base.mapper.DictTypeMapper;
import com.plating.erp.base.service.DictService;
import com.plating.erp.base.vo.DictVo;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.common.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DictServiceImpl implements DictService {

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;

    public DictServiceImpl(DictTypeMapper dictTypeMapper, DictItemMapper dictItemMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
    }

    // ==================== 字典类型 ====================

    @Override
    public List<DictVo.DictTypeListVo> listDictTypes() {
        CurrentUser me = SecurityUtils.currentUser();
        Long tenantId = me.isSystem() ? null : me.tenantId();

        LambdaQueryWrapper<DictTypeEntity> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            // 租户用户：查询系统字典(tenantId=0) + 本租户字典
            qw.and(w -> w.eq(DictTypeEntity::getTenantId, 0L)
                         .or()
                         .eq(DictTypeEntity::getTenantId, tenantId));
        }
        // 平台用户：查询所有
        qw.orderByAsc(DictTypeEntity::getId);

        List<DictTypeEntity> list = dictTypeMapper.selectList(qw);
        return list.stream().map(e -> toTypeVo(e, me)).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictVo.DictTypeListVo createDictType(DictVo.DictTypeCreateReq req) {
        CurrentUser me = SecurityUtils.currentUser();

        // 租户用户只能创建租户字典，不能创建系统字典
        Long tenantId = me.isSystem() ? 0L : me.tenantId();

        // 检查编码是否已存在（同一租户下）
        LambdaQueryWrapper<DictTypeEntity> checkQw = new LambdaQueryWrapper<>();
        checkQw.eq(DictTypeEntity::getTenantId, tenantId)
               .eq(DictTypeEntity::getDictType, req.dictType());
        if (dictTypeMapper.selectCount(checkQw) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "字典类型编码已存在");
        }

        DictTypeEntity entity = new DictTypeEntity();
        entity.setTenantId(tenantId);
        entity.setDictType(req.dictType());
        entity.setDictName(req.dictName());
        entity.setRemark(req.remark());
        entity.setStatus(0);
        entity.setCreatedBy(me.userId());
        entity.setUpdatedBy(me.userId());

        dictTypeMapper.insert(entity);
        return toTypeVo(entity, me);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictVo.DictTypeListVo updateDictType(Long id, DictVo.DictTypeUpdateReq req) {
        CurrentUser me = SecurityUtils.currentUser();
        DictTypeEntity existing = assertDictTypeAccess(id, me);

        // 系统字典只能由平台用户编辑
        if (existing.getTenantId() == 0L && !me.isSystem()) {
            throw new BizException(ErrorCode.FORBIDDEN, "系统字典不允许编辑");
        }

        LambdaUpdateWrapper<DictTypeEntity> uw = new LambdaUpdateWrapper<>();
        uw.eq(DictTypeEntity::getId, id);
        if (req.dictName() != null) {
            uw.set(DictTypeEntity::getDictName, req.dictName());
        }
        if (req.status() != null) {
            uw.set(DictTypeEntity::getStatus, req.status());
        }
        if (req.remark() != null) {
            uw.set(DictTypeEntity::getRemark, req.remark());
        }
        uw.set(DictTypeEntity::getUpdatedBy, me.userId());
        dictTypeMapper.update(null, uw);

        DictTypeEntity updated = dictTypeMapper.selectById(id);
        return toTypeVo(updated, me);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDictType(Long id) {
        CurrentUser me = SecurityUtils.currentUser();
        DictTypeEntity existing = assertDictTypeAccess(id, me);

        // 系统字典不允许删除
        if (existing.getTenantId() == 0L) {
            throw new BizException(ErrorCode.FORBIDDEN, "系统字典不允许删除");
        }

        // 删除该类型下的所有字典项
        LambdaQueryWrapper<DictItemEntity> itemQw = new LambdaQueryWrapper<>();
        itemQw.eq(DictItemEntity::getDictType, existing.getDictType());
        if (!me.isSystem()) {
            itemQw.eq(DictItemEntity::getTenantId, me.tenantId());
        }
        dictItemMapper.delete(itemQw);

        // 删除字典类型
        return dictTypeMapper.deleteById(id) > 0;
    }

    // ==================== 字典项 ====================

    @Override
    public List<DictVo.DictItemListVo> listDictItems(String dictType) {
        CurrentUser me = SecurityUtils.currentUser();
        Long tenantId = me.isSystem() ? null : me.tenantId();

        LambdaQueryWrapper<DictItemEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(DictItemEntity::getDictType, dictType);
        if (tenantId != null) {
            // 租户用户：查询系统字典项(tenantId=0) + 本租户字典项
            qw.and(w -> w.eq(DictItemEntity::getTenantId, 0L)
                         .or()
                         .eq(DictItemEntity::getTenantId, tenantId));
        }
        qw.orderByAsc(DictItemEntity::getSortNo);

        List<DictItemEntity> list = dictItemMapper.selectList(qw);
        return list.stream().map(e -> toItemVo(e, me)).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictVo.DictItemListVo createDictItem(DictVo.DictItemCreateReq req) {
        CurrentUser me = SecurityUtils.currentUser();

        Long tenantId = me.isSystem() ? 0L : me.tenantId();

        // 检查字典类型是否存在且可访问
        LambdaQueryWrapper<DictTypeEntity> typeQw = new LambdaQueryWrapper<>();
        typeQw.eq(DictTypeEntity::getDictType, req.dictType());
        if (!me.isSystem()) {
            typeQw.and(w -> w.eq(DictTypeEntity::getTenantId, 0L)
                              .or()
                              .eq(DictTypeEntity::getTenantId, me.tenantId()));
        }
        DictTypeEntity dictType = dictTypeMapper.selectOne(typeQw);
        if (dictType == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "字典类型不存在");
        }

        // 系统字典类型下不能新增字典项（由租户用户操作）
        if (dictType.getTenantId() == 0L && !me.isSystem()) {
            throw new BizException(ErrorCode.FORBIDDEN, "系统字典不允许新增字典项");
        }

        // 检查键值是否重复
        LambdaQueryWrapper<DictItemEntity> checkQw = new LambdaQueryWrapper<>();
        checkQw.eq(DictItemEntity::getTenantId, tenantId)
               .eq(DictItemEntity::getDictType, req.dictType())
               .eq(DictItemEntity::getDictValue, req.dictValue());
        if (dictItemMapper.selectCount(checkQw) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "字典键值已存在");
        }

        DictItemEntity entity = new DictItemEntity();
        entity.setTenantId(tenantId);
        entity.setDictType(req.dictType());
        entity.setDictLabel(req.dictLabel());
        entity.setDictValue(req.dictValue());
        entity.setSortNo(req.sortNo() != null ? req.sortNo() : 0);
        entity.setStatus(0);
        entity.setCreatedBy(me.userId());
        entity.setUpdatedBy(me.userId());

        dictItemMapper.insert(entity);
        return toItemVo(entity, me);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictVo.DictItemListVo updateDictItem(Long id, DictVo.DictItemUpdateReq req) {
        CurrentUser me = SecurityUtils.currentUser();
        DictItemEntity existing = assertDictItemAccess(id, me);

        // 系统字典项只能由平台用户编辑
        if (existing.getTenantId() == 0L && !me.isSystem()) {
            throw new BizException(ErrorCode.FORBIDDEN, "系统字典项不允许编辑");
        }

        LambdaUpdateWrapper<DictItemEntity> uw = new LambdaUpdateWrapper<>();
        uw.eq(DictItemEntity::getId, id);
        if (req.dictLabel() != null) {
            uw.set(DictItemEntity::getDictLabel, req.dictLabel());
        }
        if (req.dictValue() != null) {
            uw.set(DictItemEntity::getDictValue, req.dictValue());
        }
        if (req.sortNo() != null) {
            uw.set(DictItemEntity::getSortNo, req.sortNo());
        }
        if (req.status() != null) {
            uw.set(DictItemEntity::getStatus, req.status());
        }
        uw.set(DictItemEntity::getUpdatedBy, me.userId());
        dictItemMapper.update(null, uw);

        DictItemEntity updated = dictItemMapper.selectById(id);
        return toItemVo(updated, me);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDictItem(Long id) {
        CurrentUser me = SecurityUtils.currentUser();
        DictItemEntity existing = assertDictItemAccess(id, me);

        // 系统字典项不允许删除
        if (existing.getTenantId() == 0L) {
            throw new BizException(ErrorCode.FORBIDDEN, "系统字典项不允许删除");
        }

        return dictItemMapper.deleteById(id) > 0;
    }

    // ==================== 业务查询 ====================

    @Override
    public List<DictVo.DictDataVo> getDictData(String dictType) {
        CurrentUser me = SecurityUtils.currentUser();
        Long tenantId = me.isSystem() ? null : me.tenantId();

        LambdaQueryWrapper<DictItemEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(DictItemEntity::getDictType, dictType)
          .eq(DictItemEntity::getStatus, 0);  // 只返回正常状态
        if (tenantId != null) {
            qw.and(w -> w.eq(DictItemEntity::getTenantId, 0L)
                         .or()
                         .eq(DictItemEntity::getTenantId, tenantId));
        }
        qw.orderByAsc(DictItemEntity::getSortNo);

        List<DictItemEntity> list = dictItemMapper.selectList(qw);
        return list.stream()
                .map(e -> new DictVo.DictDataVo(e.getDictLabel(), e.getDictValue()))
                .toList();
    }

    // ==================== 私有方法 ====================

    private DictTypeEntity assertDictTypeAccess(Long id, CurrentUser me) {
        DictTypeEntity entity = dictTypeMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "字典类型不存在");
        }
        // 平台用户可以访问所有
        if (me.isSystem()) {
            return entity;
        }
        // 租户用户只能访问系统字典和自己的字典
        if (entity.getTenantId() != 0L && !entity.getTenantId().equals(me.tenantId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该字典类型");
        }
        return entity;
    }

    private DictItemEntity assertDictItemAccess(Long id, CurrentUser me) {
        DictItemEntity entity = dictItemMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "字典项不存在");
        }
        if (me.isSystem()) {
            return entity;
        }
        if (entity.getTenantId() != 0L && !entity.getTenantId().equals(me.tenantId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该字典项");
        }
        return entity;
    }

    private boolean isSystemDict(Long tenantId) {
        return tenantId != null && tenantId == 0L;
    }

    private boolean canEdit(DictTypeEntity entity, CurrentUser me) {
        // 平台用户可以编辑所有
        if (me.isSystem()) return true;
        // 系统字典租户不可编辑
        if (entity.getTenantId() == 0L) return false;
        // 租户只能编辑自己的字典
        return entity.getTenantId().equals(me.tenantId());
    }

    private boolean canEdit(DictItemEntity entity, CurrentUser me) {
        if (me.isSystem()) return true;
        if (entity.getTenantId() == 0L) return false;
        return entity.getTenantId().equals(me.tenantId());
    }

    private DictVo.DictTypeListVo toTypeVo(DictTypeEntity e, CurrentUser me) {
        boolean system = isSystemDict(e.getTenantId());
        boolean editable = canEdit(e, me);
        return new DictVo.DictTypeListVo(
                e.getId(), e.getDictType(), e.getDictName(), e.getStatus(),
                e.getRemark(), e.getTenantId(), system, editable,
                e.getCreatedBy(), null, e.getCreatedAt(),
                e.getUpdatedBy(), null, e.getUpdatedAt()
        );
    }

    private DictVo.DictItemListVo toItemVo(DictItemEntity e, CurrentUser me) {
        boolean system = isSystemDict(e.getTenantId());
        boolean editable = canEdit(e, me);
        return new DictVo.DictItemListVo(
                e.getId(), e.getDictType(), e.getDictLabel(), e.getDictValue(),
                e.getSortNo(), e.getStatus(), e.getTenantId(), system, editable,
                e.getCreatedBy(), null, e.getCreatedAt(),
                e.getUpdatedBy(), null, e.getUpdatedAt()
        );
    }
}