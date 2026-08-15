package com.plating.erp.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plating.erp.biz.entity.*;
import com.plating.erp.biz.enums.GoodsChangeTypeEnum;
import com.plating.erp.biz.mapper.*;
import com.plating.erp.biz.service.GoodsVersionService;
import com.plating.erp.biz.vo.GoodsOrderVo;
import com.plating.erp.biz.vo.GoodsVersionVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 版本管理Service实现
 *
 * @author Plating ERP Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsVersionServiceImpl implements GoodsVersionService {

    private final GoodsOrderVersionMapper orderVersionMapper;
    private final GoodsNodeVersionMapper nodeVersionMapper;
    private final GoodsChangeLogMapper changeLogMapper;
    private final GoodsOrderMapper orderMapper;
    private final GoodsOrderItemMapper itemMapper;
    private final GoodsProcessNodeMapper nodeMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<GoodsVersionVo.OrderVersionHistoryVo> getOrderVersions(Long orderId, Long tenantId) {
        LambdaQueryWrapper<GoodsOrderVersionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsOrderVersionEntity::getOrderId, orderId)
               .eq(GoodsOrderVersionEntity::getTenantId, tenantId)
               .orderByDesc(GoodsOrderVersionEntity::getVersionNo);
        List<GoodsOrderVersionEntity> versions = orderVersionMapper.selectList(wrapper);
        return versions.stream().map(v -> new GoodsVersionVo.OrderVersionHistoryVo(
                v.getId(), v.getVersionNo(), v.getStatus(), v.getChangeSummary(),
                v.getChangedBy(), null, v.getChangedAt(), v.getFullSnapshot()
        )).toList();
    }

    @Override
    public List<GoodsVersionVo.NodeVersionHistoryVo> getNodeVersions(Long nodeId, Long tenantId) {
        LambdaQueryWrapper<GoodsNodeVersionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsNodeVersionEntity::getNodeId, nodeId)
               .eq(GoodsNodeVersionEntity::getTenantId, tenantId)
               .orderByDesc(GoodsNodeVersionEntity::getVersionNo);
        List<GoodsNodeVersionEntity> versions = nodeVersionMapper.selectList(wrapper);
        return versions.stream().map(v -> new GoodsVersionVo.NodeVersionHistoryVo(
                v.getId(), v.getVersionNo(), v.getStatus(), v.getChangeSummary(),
                v.getChangedBy(), null, v.getChangedAt(), v.getFullSnapshot()
        )).toList();
    }

    @Override
    public List<GoodsVersionVo.ChangeLogVo> getChangeLogs(Long orderId, Long tenantId) {
        LambdaQueryWrapper<GoodsChangeLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsChangeLogEntity::getOrderId, orderId)
               .eq(GoodsChangeLogEntity::getTenantId, tenantId)
               .orderByDesc(GoodsChangeLogEntity::getChangedAt);
        List<GoodsChangeLogEntity> logs = changeLogMapper.selectList(wrapper);
        return logs.stream().map(l -> {
            GoodsChangeTypeEnum changeTypeEnum = GoodsChangeTypeEnum.fromCode(l.getChangeType());
            String changeTypeName = changeTypeEnum != null ? changeTypeEnum.getLabel() : l.getChangeType();
            return new GoodsVersionVo.ChangeLogVo(
                    l.getId(), l.getChangeType(), changeTypeName, l.getNodeId(), l.getFieldName(),
                    l.getOldValue(), l.getNewValue(), l.getChangedBy(), null,
                    l.getRemark(), l.getChangedAt()
            );
        }).toList();
    }

    @Override
    public List<GoodsVersionVo.ChangeLogVo> getChangeLogsByNode(Long nodeId, Long tenantId) {
        LambdaQueryWrapper<GoodsChangeLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsChangeLogEntity::getNodeId, nodeId)
               .eq(GoodsChangeLogEntity::getTenantId, tenantId)
               .orderByDesc(GoodsChangeLogEntity::getChangedAt);
        List<GoodsChangeLogEntity> logs = changeLogMapper.selectList(wrapper);
        return logs.stream().map(l -> {
            GoodsChangeTypeEnum changeTypeEnum = GoodsChangeTypeEnum.fromCode(l.getChangeType());
            String changeTypeName = changeTypeEnum != null ? changeTypeEnum.getLabel() : l.getChangeType();
            return new GoodsVersionVo.ChangeLogVo(
                    l.getId(), l.getChangeType(), changeTypeName, l.getNodeId(), l.getFieldName(),
                    l.getOldValue(), l.getNewValue(), l.getChangedBy(), null,
                    l.getRemark(), l.getChangedAt()
            );
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void takeSnapshot(Long orderId, Long tenantId, Long changedBy) {
        try {
            // 1. 查询开单主记录
            GoodsOrderEntity order = orderMapper.selectById(orderId);
            if (order == null || order.getDeleted() == 1) {
                log.warn("取单不存在，跳过版本快照: orderId={}", orderId);
                return;
            }

            // 2. 查询货物明细
            LambdaQueryWrapper<GoodsOrderItemEntity> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(GoodsOrderItemEntity::getOrderId, orderId)
                       .eq(GoodsOrderItemEntity::getDeleted, 0);
            List<GoodsOrderItemEntity> items = itemMapper.selectList(itemWrapper);

            // 3. 构建完整快照 JSON
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("order", toOrderMap(order));
            snapshot.put("items", items.stream().map(this::toItemMap).toList());

            // 4. 查询版本号
            LambdaQueryWrapper<GoodsOrderVersionEntity> verWrapper = new LambdaQueryWrapper<>();
            verWrapper.eq(GoodsOrderVersionEntity::getOrderId, orderId)
                      .eq(GoodsOrderVersionEntity::getTenantId, tenantId);
            long maxVer = orderVersionMapper.selectList(verWrapper).stream()
                    .mapToInt(GoodsOrderVersionEntity::getVersionNo).max().orElse(0);

            // 5. 保存版本记录
            GoodsOrderVersionEntity version = new GoodsOrderVersionEntity();
            version.setTenantId(tenantId);
            version.setOrderId(orderId);
            version.setVersionNo((int) (maxVer + 1));
            version.setOrderNo(order.getOrderNo());
            version.setCustomerId(order.getCustomerId());
            version.setCustomerName(order.getCustomerName());
            version.setDepartmentId(order.getDepartmentId());
            version.setDepartmentName(order.getDepartmentName());
            version.setStatus(order.getStatus());
            version.setTotalItems(order.getTotalItems());
            version.setTotalQuantity(order.getTotalQuantity());
            version.setRemark(order.getRemark());
            version.setOperatorId(order.getOperatorId());
            version.setOperatorName(order.getOperatorName());
            version.setChangedBy(changedBy);
            version.setChangedAt(LocalDateTime.now());
            version.setChangeSummary("保存版本快照 #" + (maxVer + 1));
            version.setFullSnapshot(toJson(snapshot));

            orderVersionMapper.insert(version);
            log.info("保存开单版本快照成功: orderId={}, version={}", orderId, version.getVersionNo());
        } catch (JsonProcessingException e) {
            log.error("保存开单版本快照JSON序列化失败: orderId={}", orderId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void takeNodeSnapshot(Long nodeId, Long tenantId, Long changedBy) {
        try {
            GoodsProcessNodeEntity node = nodeMapper.selectById(nodeId);
            if (node == null || node.getDeleted() == 1) {
                log.warn("节点不存在，跳过版本快照: nodeId={}", nodeId);
                return;
            }

            // 查询版本号
            LambdaQueryWrapper<GoodsNodeVersionEntity> verWrapper = new LambdaQueryWrapper<>();
            verWrapper.eq(GoodsNodeVersionEntity::getNodeId, nodeId)
                      .eq(GoodsNodeVersionEntity::getTenantId, tenantId);
            long maxVer = nodeVersionMapper.selectList(verWrapper).stream()
                    .mapToInt(GoodsNodeVersionEntity::getVersionNo).max().orElse(0);

            // 保存版本记录
            GoodsNodeVersionEntity version = new GoodsNodeVersionEntity();
            version.setTenantId(tenantId);
            version.setNodeId(nodeId);
            version.setOrderId(node.getOrderId());
            version.setOrderNo(node.getOrderNo());
            version.setItemId(node.getItemId());
            version.setItemName(node.getItemName());
            version.setDepartmentId(node.getDepartmentId());
            version.setDepartmentName(node.getDepartmentName());
            version.setNodeOrder(node.getNodeOrder());
            version.setStatus(node.getStatus());
            version.setOriginalQuantity(node.getOriginalQuantity());
            version.setProcessedQuantity(node.getProcessedQuantity());
            version.setLossQuantity(node.getLossQuantity());
            version.setLossRate(node.getLossRate());
            version.setRemark(node.getRemark());
            version.setOperatorId(node.getOperatorId());
            version.setOperatorName(node.getOperatorName());
            version.setProcessedAt(node.getProcessedAt());
            version.setVersionNo((int) (maxVer + 1));
            version.setChangedBy(changedBy);
            version.setChangedAt(LocalDateTime.now());
            version.setChangeSummary("保存节点版本快照 #" + (maxVer + 1));

            // 完整快照
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("node", toNodeMap(node));
            version.setFullSnapshot(toJson(snapshot));

            nodeVersionMapper.insert(version);
            log.info("保存节点版本快照成功: nodeId={}, version={}", nodeId, version.getVersionNo());
        } catch (JsonProcessingException e) {
            log.error("保存节点版本快照JSON序列化失败: nodeId={}", nodeId, e);
        }
    }

    // ========== 私有辅助方法 ==========

    private Map<String, Object> toOrderMap(GoodsOrderEntity order) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", order.getId());
        m.put("orderNo", order.getOrderNo());
        m.put("orderDate", order.getOrderDate());
        m.put("customerId", order.getCustomerId());
        m.put("customerName", order.getCustomerName());
        m.put("departmentId", order.getDepartmentId());
        m.put("departmentName", order.getDepartmentName());
        m.put("status", order.getStatus());
        m.put("totalItems", order.getTotalItems());
        m.put("totalQuantity", order.getTotalQuantity());
        m.put("remark", order.getRemark());
        m.put("operatorId", order.getOperatorId());
        m.put("operatorName", order.getOperatorName());
        m.put("submittedAt", order.getSubmittedAt());
        m.put("distributedAt", order.getDistributedAt());
        m.put("completedAt", order.getCompletedAt());
        return m;
    }

    private Map<String, Object> toItemMap(GoodsOrderItemEntity item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", item.getId());
        m.put("itemName", item.getItemName());
        m.put("quantity", item.getQuantity());
        m.put("unit", item.getUnit());
        m.put("unitPrice", item.getUnitPrice());
        m.put("totalPrice", item.getTotalPrice());
        m.put("specification", item.getSpecification());
        m.put("material", item.getMaterial());
        m.put("photoUrls", item.getPhotoUrls());
        m.put("remark", item.getRemark());
        return m;
    }

    private Map<String, Object> toNodeMap(GoodsProcessNodeEntity node) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", node.getId());
        m.put("orderId", node.getOrderId());
        m.put("orderNo", node.getOrderNo());
        m.put("itemId", node.getItemId());
        m.put("itemName", node.getItemName());
        m.put("departmentId", node.getDepartmentId());
        m.put("departmentName", node.getDepartmentName());
        m.put("nodeOrder", node.getNodeOrder());
        m.put("status", node.getStatus());
        m.put("originalQuantity", node.getOriginalQuantity());
        m.put("processedQuantity", node.getProcessedQuantity());
        m.put("lossQuantity", node.getLossQuantity());
        m.put("lossRate", node.getLossRate());
        m.put("remark", node.getRemark());
        m.put("operatorId", node.getOperatorId());
        m.put("operatorName", node.getOperatorName());
        m.put("processedAt", node.getProcessedAt());
        m.put("rollbackCount", node.getRollbackCount());
        m.put("rollbackReason", node.getRollbackReason());
        return m;
    }

    private String toJson(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }
}
