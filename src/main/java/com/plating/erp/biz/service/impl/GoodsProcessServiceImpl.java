package com.plating.erp.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.biz.entity.GoodsChangeLogEntity;
import com.plating.erp.biz.entity.GoodsImageEntity;
import com.plating.erp.biz.entity.GoodsOrderEntity;
import com.plating.erp.biz.entity.GoodsOrderItemEntity;
import com.plating.erp.biz.entity.GoodsProcessNodeEntity;
import com.plating.erp.biz.entity.GoodsProcessRecordEntity;
import com.plating.erp.biz.enums.GoodsChangeTypeEnum;
import com.plating.erp.biz.enums.GoodsNodeStatusEnum;
import com.plating.erp.biz.enums.GoodsOrderStatusEnum;
import com.plating.erp.biz.mapper.*;
import com.plating.erp.biz.service.GoodsImageAiService;
import com.plating.erp.biz.service.GoodsProcessService;
import com.plating.erp.biz.service.GoodsVersionService;
import com.plating.erp.biz.vo.GoodsProcessVo;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 货物加工Service实现
 *
 * @author Plating ERP Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsProcessServiceImpl implements GoodsProcessService {

    private final GoodsProcessNodeMapper nodeMapper;
    private final GoodsProcessRecordMapper recordMapper;
    private final GoodsOrderMapper orderMapper;
    private final GoodsOrderItemMapper itemMapper;
    private final GoodsImageMapper imageMapper;
    private final GoodsChangeLogMapper changeLogMapper;
    private final GoodsVersionService versionService;
    private final GoodsImageAiService imageAiService;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GoodsProcessVo.ProcessRecordVo submitProcess(GoodsProcessVo.ProcessSubmitReq req) {
        // 查询节点
        GoodsProcessNodeEntity node = nodeMapper.selectById(req.nodeId());
        if (node == null || node.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "加工节点不存在");
        }

        // 校验节点状态：只有待处理或加工中的节点可以录入
        if (!GoodsNodeStatusEnum.PENDING.getCode().equals(node.getStatus())
                && !GoodsNodeStatusEnum.PROCESSING.getCode().equals(node.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该节点已录入加工数据，不可重复录入");
        }

        // 校验操作人必须属于当前节点所属部门
        validateDepartmentAccess(node, "录入");

        // 校验上一节点必须已完成（第一个节点除外）
        validatePreviousNodeCompleted(node);

        // 校验加工数量不能大于原始数量
        if (req.processedQuantity().compareTo(node.getOriginalQuantity()) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "加工后数量（" + req.processedQuantity() + "）不能大于原始数量（" + node.getOriginalQuantity() + "）");
        }
        if (req.processedQuantity().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "加工后数量不能为负数");
        }

        // 保存版本快照（变更前）
        saveNodeVersionSnapshot(node, req.operatorId(), "录入加工数据");

        // 创建加工记录
        GoodsProcessRecordEntity record = new GoodsProcessRecordEntity();
        record.setTenantId(node.getTenantId());
        record.setNodeId(node.getId());
        record.setOrderId(node.getOrderId());
        record.setOrderNo(node.getOrderNo());
        record.setItemId(node.getItemId());
        record.setItemName(node.getItemName());
        record.setDepartmentId(node.getDepartmentId());
        record.setDepartmentName(node.getDepartmentName());
        record.setNodeOrder(node.getNodeOrder());
        record.setProcessedQuantity(req.processedQuantity());
        record.setBeforePhotoUrls(getBeforePhotos(node.getItemId()));
        record.setAfterPhotoUrls(req.afterPhotoUrls());
        record.setLossQuantity(node.getOriginalQuantity().subtract(req.processedQuantity()));
        record.setLossRate(calculateLossRate(node.getOriginalQuantity(), record.getLossQuantity()));
        record.setRemark(req.remark());
        record.setOperatorId(req.operatorId());
        record.setOperatorName(req.operatorName());
        record.setProcessedAt(LocalDateTime.now());
        record.setCreatedBy(req.operatorId());
        recordMapper.insert(record);

        // 更新节点状态为加工完成
        node.setProcessedQuantity(req.processedQuantity());
        node.setLossQuantity(record.getLossQuantity());
        node.setLossRate(record.getLossRate());
        node.setStatus(GoodsNodeStatusEnum.PROCESSED.getCode());
        node.setOperatorId(req.operatorId());
        node.setOperatorName(req.operatorName());
        node.setProcessedAt(LocalDateTime.now());
        nodeMapper.updateById(node);

        // 记录变更日志：将本次操作所有字段变更合并为一条记录
        Map<String, Object> oldValues = new LinkedHashMap<>();
        Map<String, Object> newValues = new LinkedHashMap<>();
        oldValues.put("processedQuantity", null);
        newValues.put("processedQuantity", req.processedQuantity());
        oldValues.put("lossQuantity", null);
        newValues.put("lossQuantity", record.getLossQuantity());
        if (req.remark() != null && !req.remark().isBlank()) {
            oldValues.put("remark", null);
            newValues.put("remark", req.remark());
        }
        saveChangeLog(node.getOrderId(), node.getOrderNo(), node.getId(),
                GoodsChangeTypeEnum.NODE_UPDATE,
                oldValues, newValues,
                "录入加工数据", req.operatorId(), req.operatorName());

        // 检查是否所有节点都完成了
        checkAllNodesCompleted(node.getOrderId(), node.getTenantId());

        log.info("加工数据录入成功: nodeId={}, recordId={}", node.getId(), record.getId());
        return toProcessRecordVo(record, node);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeNodeStatus(GoodsProcessVo.NodeStatusChangeReq req) {
        GoodsProcessNodeEntity node = nodeMapper.selectById(req.nodeId());
        if (node == null || node.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "加工节点不存在");
        }

        GoodsNodeStatusEnum current = GoodsNodeStatusEnum.fromCode(node.getStatus());
        GoodsNodeStatusEnum target = GoodsNodeStatusEnum.fromCode(req.targetStatus());

        if (current == null || target == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "无效的状态值");
        }

        // 状态机校验：只能单向推进
        if (!current.canTransitionTo(target)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "不允许从 " + current.getLabel() + " 跳转到 " + target.getLabel());
        }

        // 校验操作人必须属于当前节点所属部门
        validateDepartmentAccess(node, "变更状态");

        // 校验上一节点必须已完成（第一个节点除外）
        validatePreviousNodeCompleted(node);

        // 仅在数据变化时保存版本快照（状态变更不保存快照）
        // saveNodeVersionSnapshot 仅在 submitProcess 等数据变更场景调用

        node.setStatus(req.targetStatus());
        node.setUpdatedBy(req.operatorId());
        nodeMapper.updateById(node);

        saveChangeLog(node.getOrderId(), node.getOrderNo(), node.getId(),
                GoodsChangeTypeEnum.NODE_STATUS_CHANGE,
                "status", current.getCode(), target.getCode(),
                "节点状态变更: " + current.getLabel() + " → " + target.getLabel(),
                req.operatorId(), req.operatorName());

        // 检查最后一个节点是否完成
        checkAllNodesCompleted(node.getOrderId(), node.getTenantId());

        log.info("节点状态变更成功: nodeId={}, {} → {}", node.getId(), current.getLabel(), target.getLabel());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackNode(GoodsProcessVo.NodeRollbackReq req) {
        // 前端传入的是当前 PENDING 节点（操作发起者），回退它的前一个 PROCESSED 节点
        GoodsProcessNodeEntity currentNode = nodeMapper.selectById(req.nodeId());
        if (currentNode == null || currentNode.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "加工节点不存在");
        }

        // 校验：当前节点必须是 PENDING 状态（只有 PENDING 节点可以发起回退）
        if (!GoodsNodeStatusEnum.PENDING.getCode().equals(currentNode.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有待处理节点可以操作回退");
        }

        // 查找前驱 PROCESSED 节点（跳过 ROLLED_BACK 和同部门节点）
        LambdaQueryWrapper<GoodsProcessNodeEntity> prevWrapper = new LambdaQueryWrapper<>();
        prevWrapper.eq(GoodsProcessNodeEntity::getOrderId, currentNode.getOrderId())
                   .eq(GoodsProcessNodeEntity::getItemId, currentNode.getItemId())
                   .eq(GoodsProcessNodeEntity::getTenantId, currentNode.getTenantId())
                   .lt(GoodsProcessNodeEntity::getNodeOrder, currentNode.getNodeOrder())
                   .eq(GoodsProcessNodeEntity::getDeleted, 0)
                   .orderByDesc(GoodsProcessNodeEntity::getNodeOrder);
        List<GoodsProcessNodeEntity> prevNodes = nodeMapper.selectList(prevWrapper);
        GoodsProcessNodeEntity prevNode = null;
        for (GoodsProcessNodeEntity pn : prevNodes) {
            if (GoodsNodeStatusEnum.ROLLED_BACK.getCode().equals(pn.getStatus())) {
                continue; // 跳过已回退节点
            }
            if (GoodsNodeStatusEnum.PROCESSED.getCode().equals(pn.getStatus())) {
                // 跳过同部门的节点（如B新跳过B，找到A）
                if (pn.getDepartmentId() != null && pn.getDepartmentId().equals(currentNode.getDepartmentId())) {
                    continue;
                }
                prevNode = pn;
            }
            break; // 找到第一个非 ROLLED_BACK 节点后停止
        }
        if (prevNode == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "前方无可回退的加工完成节点");
        }

        // 权限校验：操作人必须属于当前 PENDING 节点的部门
        CurrentUser currentUser = SecurityUtils.currentUser();
        if (!currentUser.isSystem()) {
            UserEntity user = userMapper.selectById(currentUser.userId());
            if (user == null || user.getDeptId() == null) {
                throw new BizException(ErrorCode.FORBIDDEN, "无权限回退该节点");
            }
            if (!user.getDeptId().equals(currentNode.getDepartmentId())) {
                throw new BizException(ErrorCode.FORBIDDEN, "无权限回退该节点，仅当前环节部门可操作");
            }
        }

        // 将当前 PENDING 节点状态改为 ROLLED_BACK（已回退）
        currentNode.setStatus(GoodsNodeStatusEnum.ROLLED_BACK.getCode());
        nodeMapper.updateById(currentNode);

        // 新节点插入位置：紧跟在当前回退节点之后
        int insertOrder1 = currentNode.getNodeOrder() + 1;
        int insertOrder2 = currentNode.getNodeOrder() + 2;

        // 将当前节点之后的所有下游节点 nodeOrder 顺延2位（为新节点腾出位置）
        // 必须按 nodeOrder 降序处理，避免顺延时唯一约束冲突
        LambdaQueryWrapper<GoodsProcessNodeEntity> shiftWrapper = new LambdaQueryWrapper<>();
        shiftWrapper.eq(GoodsProcessNodeEntity::getOrderId, currentNode.getOrderId())
                    .eq(GoodsProcessNodeEntity::getTenantId, currentNode.getTenantId())
                    .gt(GoodsProcessNodeEntity::getNodeOrder, currentNode.getNodeOrder())
                    .orderByDesc(GoodsProcessNodeEntity::getNodeOrder);
        List<GoodsProcessNodeEntity> downstreamNodes = nodeMapper.selectList(shiftWrapper);
        for (GoodsProcessNodeEntity dn : downstreamNodes) {
            log.info("顺延节点: id={}, nodeOrder {} → {}, dept={}",
                    dn.getId(), dn.getNodeOrder(), dn.getNodeOrder() + 2, dn.getDepartmentName());
            dn.setNodeOrder(dn.getNodeOrder() + 2);
            nodeMapper.updateById(dn);
        }

        String prevDeptName = prevNode != null ? prevNode.getDepartmentName() : currentNode.getDepartmentName();
        log.info("回退开始: currentNodeId={}, nodeOrder={}, dept={}, prevNode={}",
                currentNode.getId(), currentNode.getNodeOrder(), currentNode.getDepartmentName(), prevDeptName);

        // 保存版本快照
        saveNodeVersionSnapshot(currentNode, req.operatorId(),
                "节点回退: " + currentNode.getDepartmentName() + "回退到" + prevDeptName);

        // 创建新节点1：复制上一节点(prevNode)的部门，首节点则复制自身部门
        GoodsProcessNodeEntity newNode1 = new GoodsProcessNodeEntity();
        newNode1.setTenantId(currentNode.getTenantId());
        newNode1.setOrderId(currentNode.getOrderId());
        newNode1.setOrderNo(currentNode.getOrderNo());
        newNode1.setItemId(currentNode.getItemId());
        newNode1.setItemName(currentNode.getItemName());
        newNode1.setDepartmentId(prevNode != null ? prevNode.getDepartmentId() : currentNode.getDepartmentId());
        newNode1.setDepartmentName(prevNode != null ? prevNode.getDepartmentName() : currentNode.getDepartmentName());
        newNode1.setNodeOrder(insertOrder1);
        newNode1.setOriginalOrder(null);
        newNode1.setStatus(GoodsNodeStatusEnum.PENDING.getCode());
        newNode1.setOriginalQuantity(currentNode.getOriginalQuantity());
        newNode1.setCreatedBy(req.operatorId());
        newNode1.setRollbackCount(1);
        newNode1.setRollbackReason(req.reason());
        nodeMapper.insert(newNode1);

        // 创建新节点2：复制当前回退节点(currentNode)的部门
        GoodsProcessNodeEntity newNode2 = new GoodsProcessNodeEntity();
        newNode2.setTenantId(currentNode.getTenantId());
        newNode2.setOrderId(currentNode.getOrderId());
        newNode2.setOrderNo(currentNode.getOrderNo());
        newNode2.setItemId(currentNode.getItemId());
        newNode2.setItemName(currentNode.getItemName());
        newNode2.setDepartmentId(currentNode.getDepartmentId());
        newNode2.setDepartmentName(currentNode.getDepartmentName());
        newNode2.setNodeOrder(insertOrder2);
        newNode2.setOriginalOrder(null);
        newNode2.setStatus(GoodsNodeStatusEnum.PENDING.getCode());
        newNode2.setOriginalQuantity(currentNode.getOriginalQuantity());
        newNode2.setCreatedBy(req.operatorId());
        newNode2.setRollbackCount(1);
        newNode2.setRollbackReason(req.reason());
        nodeMapper.insert(newNode2);

        // 记录变更日志
        saveChangeLog(currentNode.getOrderId(), currentNode.getOrderNo(), currentNode.getId(),
                GoodsChangeTypeEnum.ROLLBACK,
                "rollback", null, "insert:" + newNode1.getId() + "," + newNode2.getId(),
                "节点回退: " + currentNode.getDepartmentName() + "(节点" + currentNode.getNodeOrder() + ")回退到"
                        + prevDeptName + "，插入新节点 "
                        + newNode1.getDepartmentName() + "(节点" + newNode1.getNodeOrder() + ") → "
                        + newNode2.getDepartmentName() + "(节点" + newNode2.getNodeOrder() + ")，原因: " + req.reason(),
                req.operatorId(), req.operatorName());

        log.info("节点回退成功: {}回退到{}, 在节点{}后插入新节点{}({})→{}({}), 下游{}个节点nodeOrder顺延+2",
                currentNode.getDepartmentName(), prevDeptName,
                currentNode.getNodeOrder(),
                newNode1.getNodeOrder(), newNode1.getDepartmentName(),
                newNode2.getNodeOrder(), newNode2.getDepartmentName(),
                downstreamNodes.size());
    }

    @Override
    public List<GoodsProcessVo.ProcessRecordVo> getNodesByOrderId(Long orderId, Long tenantId) {
        LambdaQueryWrapper<GoodsProcessNodeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsProcessNodeEntity::getOrderId, orderId)
               .eq(GoodsProcessNodeEntity::getTenantId, tenantId)
               .eq(GoodsProcessNodeEntity::getDeleted, 0)
               .orderByAsc(GoodsProcessNodeEntity::getItemId)
               .orderByAsc(GoodsProcessNodeEntity::getNodeOrder);
        List<GoodsProcessNodeEntity> nodes = nodeMapper.selectList(wrapper);
        return nodes.stream().map(n -> toProcessRecordVo(null, n)).toList();
    }

    @Override
    public GoodsProcessVo.ProcessRecordVo getRecordByNodeId(Long nodeId, Long tenantId) {
        LambdaQueryWrapper<GoodsProcessRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsProcessRecordEntity::getNodeId, nodeId)
               .eq(GoodsProcessRecordEntity::getTenantId, tenantId)
               .eq(GoodsProcessRecordEntity::getDeleted, 0);
        GoodsProcessRecordEntity record = recordMapper.selectOne(wrapper);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "加工记录不存在");
        }
        return toProcessRecordVo(record, null);
    }

    @Override
    public PageResult<GoodsProcessVo.GoodsSearchResultVo> searchGoods(GoodsProcessVo.GoodsSearchReq req,
                                                                       Long tenantId) {
        // 组合查询：开单编号 + 客户名称 + 货物名称 + 状态
        LambdaQueryWrapper<GoodsOrderEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsOrderEntity::getTenantId, tenantId)
               .eq(GoodsOrderEntity::getDeleted, 0);
        if (req.keyword() != null && !req.keyword().isBlank()) {
            wrapper.and(w -> w.like(GoodsOrderEntity::getOrderNo, req.keyword())
                    .or().like(GoodsOrderEntity::getCustomerName, req.keyword()));
        }
        if (req.customerId() != null) {
            wrapper.eq(GoodsOrderEntity::getCustomerId, req.customerId());
        }
        if (req.status() != null && !req.status().isBlank()) {
            wrapper.eq(GoodsOrderEntity::getStatus, req.status());
        }
        wrapper.orderByDesc(GoodsOrderEntity::getId);

        Page<GoodsOrderEntity> page = orderMapper.selectPage(new Page<>(req.page(), req.size()), wrapper);
        List<GoodsProcessVo.GoodsSearchResultVo> vos = page.getRecords().stream().map(o ->
                new GoodsProcessVo.GoodsSearchResultVo(
                        o.getId(), o.getOrderNo(), o.getCustomerName(),
                        o.getStatus(), o.getTotalItems(), o.getCreatedAt()
                )
        ).toList();

        return new PageResult<>(vos, page.getTotal());
    }

    @Override
    public List<GoodsProcessVo.ImageSearchResultVo> searchByImage(String imageUrl, Double threshold,
                                                                   Long tenantId) {
        // 调用AI服务进行图片相似度搜索
        return imageAiService.searchSimilarImages(imageUrl, threshold, tenantId).stream().map(m ->
                new GoodsProcessVo.ImageSearchResultVo(
                        m.itemId(), m.orderNo(), m.itemName(),
                        m.similarity(), m.thumbnailUrl()
                )
        ).toList();
    }

    @Override
    public List<GoodsProcessVo.ProcessRecordVo> getProcessRecordsByOrderId(Long orderId, Long tenantId) {
        LambdaQueryWrapper<GoodsProcessRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsProcessRecordEntity::getOrderId, orderId)
               .eq(GoodsProcessRecordEntity::getTenantId, tenantId)
               .eq(GoodsProcessRecordEntity::getDeleted, 0)
               .orderByAsc(GoodsProcessRecordEntity::getProcessedAt);
        List<GoodsProcessRecordEntity> records = recordMapper.selectList(wrapper);
        return records.stream().map(r -> toProcessRecordVo(r, null)).toList();
    }

    @Override
    public List<Map<String, Object>> getBusinessData(List<Long> itemIds, Long tenantId) {
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }
        // 通过 item_id 查询明细，关联获取 order_no 和 customer_name
        LambdaQueryWrapper<GoodsOrderItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(GoodsOrderItemEntity::getId, itemIds)
               .eq(GoodsOrderItemEntity::getTenantId, tenantId)
               .eq(GoodsOrderItemEntity::getDeleted, 0);
        List<GoodsOrderItemEntity> items = itemMapper.selectList(wrapper);

        // 批量查询开单主表获取 order_no 和 customer_name
        Set<Long> orderIds = items.stream()
                .map(GoodsOrderItemEntity::getOrderId)
                .collect(Collectors.toSet());
        Map<Long, GoodsOrderEntity> orderMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            LambdaQueryWrapper<GoodsOrderEntity> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.in(GoodsOrderEntity::getId, orderIds)
                    .eq(GoodsOrderEntity::getTenantId, tenantId)
                    .eq(GoodsOrderEntity::getDeleted, 0);
            List<GoodsOrderEntity> orders = orderMapper.selectList(orderWrapper);
            orders.forEach(o -> orderMap.put(o.getId(), o));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (GoodsOrderItemEntity item : items) {
            GoodsOrderEntity order = orderMap.get(item.getOrderId());
            Map<String, Object> data = new HashMap<>();
            data.put("itemId", item.getId());
            data.put("orderNo", order != null ? order.getOrderNo() : "");
            data.put("itemName", item.getItemName());
            data.put("customerName", order != null ? order.getCustomerName() : "");
            result.add(data);
        }
        return result;
    }

    // ========== 私有方法 ==========

    /**
     * 校验当前操作人是否属于节点所属部门
     */
    private void validateDepartmentAccess(GoodsProcessNodeEntity node, String operation) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        // 平台用户（系统管理员）可以操作所有节点
        if (currentUser.isSystem()) {
            return;
        }
        UserEntity user = userMapper.selectById(currentUser.userId());
        if (user == null || user.getDeptId() == null
                || !user.getDeptId().equals(node.getDepartmentId())) {
            throw new BizException(ErrorCode.FORBIDDEN,
                    "无权限" + operation + "该节点，您不属于节点所属部门");
        }
    }

    private List<String> getBeforePhotos(Long itemId) {
        LambdaQueryWrapper<GoodsImageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsImageEntity::getItemId, itemId)
               .eq(GoodsImageEntity::getImageType, "SAMPLE");
        List<GoodsImageEntity> images = imageMapper.selectList(wrapper);
        return images.stream().map(GoodsImageEntity::getImageUrl).toList();
    }

    private BigDecimal calculateLossRate(BigDecimal original, BigDecimal loss) {
        if (original == null || original.compareTo(BigDecimal.ZERO) == 0 || loss == null) {
            return BigDecimal.ZERO;
        }
        return loss.divide(original, 4, BigDecimal.ROUND_HALF_UP);
    }

    private void checkAllNodesCompleted(Long orderId, Long tenantId) {
        LambdaQueryWrapper<GoodsProcessNodeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsProcessNodeEntity::getOrderId, orderId)
               .eq(GoodsProcessNodeEntity::getTenantId, tenantId)
               .eq(GoodsProcessNodeEntity::getDeleted, 0);
        List<GoodsProcessNodeEntity> nodes = nodeMapper.selectList(wrapper);
        if (nodes.isEmpty()) return;

        // 按 itemId 分组，检查每个货物的最后一个节点是否已加工完成（PROCESSED及以上）
        Map<Long, List<GoodsProcessNodeEntity>> nodesByItem = nodes.stream()
                .collect(java.util.stream.Collectors.groupingBy(GoodsProcessNodeEntity::getItemId));

        boolean allLastNodesProcessed = nodesByItem.values().stream().allMatch(itemNodes -> {
            // 找到最后一个非 ROLLED_BACK 节点（最大 nodeOrder）
            GoodsProcessNodeEntity lastNode = itemNodes.stream()
                    .filter(n -> !GoodsNodeStatusEnum.ROLLED_BACK.getCode().equals(n.getStatus()))
                    .max(java.util.Comparator.comparingInt(GoodsProcessNodeEntity::getNodeOrder))
                    .orElse(null);
            if (lastNode == null) return false;
            String status = lastNode.getStatus();
            return GoodsNodeStatusEnum.PROCESSED.getCode().equals(status)
                    || GoodsNodeStatusEnum.COMPLETED.getCode().equals(status);
        });

        if (allLastNodesProcessed) {
            GoodsOrderEntity order = orderMapper.selectById(orderId);
            if (order != null && !GoodsOrderStatusEnum.COMPLETED.getCode().equals(order.getStatus())) {
                String oldStatus = order.getStatus();
                order.setStatus(GoodsOrderStatusEnum.COMPLETED.getCode());
                order.setCompletedAt(LocalDateTime.now());
                orderMapper.updateById(order);
                saveChangeLog(orderId, order.getOrderNo(), null,
                        GoodsChangeTypeEnum.STATUS_AUTO_UPDATE,
                        "status", oldStatus, GoodsOrderStatusEnum.COMPLETED.getCode(),
                        "所有货物最后一个节点加工完成，自动更新开单状态为已完成", null, null);
                log.info("开单自动完成: orderId={}", orderId);
            }
        }
    }

    /**
     * 回退时重置上一节点为待处理，实现完整流转链路。
     * 例如 A→B→C→D，在C处回退，则重置B为PENDING，后续可继续 B→C→D
     * 仅重置同一货物（itemId）的上一个顺序节点
     */
    private void resetPreviousNode(Long orderId, Long itemId, Integer currentNodeOrder, Long tenantId) {
        LambdaQueryWrapper<GoodsProcessNodeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsProcessNodeEntity::getOrderId, orderId)
               .eq(GoodsProcessNodeEntity::getItemId, itemId)
               .eq(GoodsProcessNodeEntity::getTenantId, tenantId)
               .eq(GoodsProcessNodeEntity::getNodeOrder, currentNodeOrder - 1)
               .eq(GoodsProcessNodeEntity::getDeleted, 0);
        GoodsProcessNodeEntity prevNode = nodeMapper.selectOne(wrapper);
        if (prevNode != null) {
            prevNode.setStatus(GoodsNodeStatusEnum.PENDING.getCode());
            prevNode.setProcessedQuantity(null);
            prevNode.setLossQuantity(null);
            prevNode.setLossRate(null);
            prevNode.setProcessedAt(null);
            prevNode.setOperatorId(null);
            prevNode.setOperatorName(null);
            nodeMapper.updateById(prevNode);
            log.info("回退时重置上一节点为待处理: prevNodeId={}, prevNodeOrder={}",
                    prevNode.getId(), prevNode.getNodeOrder());
        }
    }

    /**
     * 校验上一节点必须已加工完成（PROCESSED及以上），第一个节点除外。
     * 确保加工流程按顺序流转：上一个部门完成后才能流转到下一个部门。
     */
    private void validatePreviousNodeCompleted(GoodsProcessNodeEntity node) {
        if (node.getNodeOrder() == null || node.getNodeOrder() <= 1) {
            return; // 第一个节点无需校验
        }
        LambdaQueryWrapper<GoodsProcessNodeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsProcessNodeEntity::getOrderId, node.getOrderId())
               .eq(GoodsProcessNodeEntity::getItemId, node.getItemId())
               .eq(GoodsProcessNodeEntity::getTenantId, node.getTenantId())
               .eq(GoodsProcessNodeEntity::getNodeOrder, node.getNodeOrder() - 1)
               .eq(GoodsProcessNodeEntity::getDeleted, 0);
        GoodsProcessNodeEntity prevNode = nodeMapper.selectOne(wrapper);
        if (prevNode == null) {
            return; // 没有上一节点，跳过校验
        }
        GoodsNodeStatusEnum prevStatus = GoodsNodeStatusEnum.fromCode(prevNode.getStatus());
        if (prevStatus == null || prevStatus.getOrder() < GoodsNodeStatusEnum.PROCESSED.getOrder()) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "上一节点（" + prevNode.getDepartmentName() + "）尚未加工完成，无法操作当前节点");
        }
    }

    private void resetFollowingNodes(Long orderId, Long itemId, Integer currentNodeOrder, Long tenantId) {
        LambdaQueryWrapper<GoodsProcessNodeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsProcessNodeEntity::getOrderId, orderId)
               .eq(GoodsProcessNodeEntity::getItemId, itemId)
               .eq(GoodsProcessNodeEntity::getTenantId, tenantId)
               .gt(GoodsProcessNodeEntity::getNodeOrder, currentNodeOrder)
               .eq(GoodsProcessNodeEntity::getDeleted, 0);
        List<GoodsProcessNodeEntity> followingNodes = nodeMapper.selectList(wrapper);
        for (GoodsProcessNodeEntity n : followingNodes) {
            n.setStatus(GoodsNodeStatusEnum.PENDING.getCode());
            n.setProcessedQuantity(null);
            n.setLossQuantity(null);
            n.setLossRate(null);
            n.setProcessedAt(null);
            n.setOperatorId(null);
            n.setOperatorName(null);
            nodeMapper.updateById(n);
        }
        log.info("重置后续节点为待处理: count={}", followingNodes.size());
    }

    private void saveNodeVersionSnapshot(GoodsProcessNodeEntity node, Long operatorId, String changeSummary) {
        versionService.takeNodeSnapshot(node.getId(), node.getTenantId(), operatorId);
    }

    private void saveChangeLog(Long orderId, String orderNo, Long nodeId,
                                GoodsChangeTypeEnum changeType, String remark,
                                Long operatorId, String operatorName) {
        saveChangeLog(orderId, orderNo, nodeId, changeType, (Map<String, Object>) null, null,
                remark, operatorId, operatorName);
    }

    /**
     * 单字段变更日志（向后兼容）
     */
    private void saveChangeLog(Long orderId, String orderNo, Long nodeId,
                                GoodsChangeTypeEnum changeType, String fieldName,
                                Object oldValue, Object newValue,
                                String remark, Long operatorId, String operatorName) {
        Map<String, Object> oldValues = null;
        Map<String, Object> newValues = null;
        if (fieldName != null) {
            oldValues = new LinkedHashMap<>();
            oldValues.put(fieldName, oldValue);
            newValues = new LinkedHashMap<>();
            newValues.put(fieldName, newValue);
        }
        saveChangeLog(orderId, orderNo, nodeId, changeType, oldValues, newValues,
                remark, operatorId, operatorName);
    }

    /**
     * 批量字段变更日志：一次操作的所有字段变更合并为一条记录
     */
    private void saveChangeLog(Long orderId, String orderNo, Long nodeId,
                                GoodsChangeTypeEnum changeType,
                                Map<String, Object> oldValues, Map<String, Object> newValues,
                                String remark, Long operatorId, String operatorName) {
        GoodsChangeLogEntity logEntry = new GoodsChangeLogEntity();
        CurrentUser cu = SecurityUtils.currentUser();
        Long tenantId = cu != null ? cu.tenantId() : null;
        logEntry.setTenantId(tenantId);
        logEntry.setOrderId(orderId);
        logEntry.setOrderNo(orderNo);
        logEntry.setNodeId(nodeId);
        logEntry.setChangeType(changeType.getCode());
        logEntry.setOldValue(oldValues);
        logEntry.setNewValue(newValues);
        logEntry.setChangedBy(operatorId);
        logEntry.setChangedAt(LocalDateTime.now());
        logEntry.setRemark(remark);
        changeLogMapper.insert(logEntry);
    }

    private GoodsProcessVo.ProcessRecordVo toProcessRecordVo(GoodsProcessRecordEntity record,
                                                              GoodsProcessNodeEntity node) {
        if (record != null) {
            return new GoodsProcessVo.ProcessRecordVo(
                    record.getId(), record.getNodeId(), record.getOrderNo(),
                    record.getItemId(), record.getItemName(),
                    record.getDepartmentId(), record.getDepartmentName(), record.getNodeOrder(),
                    node != null ? node.getStatus() : "",
                    node != null ? node.getOriginalQuantity() : null,
                    record.getProcessedQuantity(), record.getLossQuantity(),
                    record.getLossRate(), record.getBeforePhotoUrls(),
                    record.getAfterPhotoUrls(), record.getRemark(),
                    record.getOperatorName(), record.getProcessedAt()
            );
        }
        // 只有节点没有记录的情况
        return new GoodsProcessVo.ProcessRecordVo(
                null, node.getId(), node.getOrderNo(), node.getItemId(), node.getItemName(),
                node.getDepartmentId(), node.getDepartmentName(), node.getNodeOrder(), node.getStatus(),
                node.getOriginalQuantity(), node.getProcessedQuantity(),
                node.getLossQuantity(), node.getLossRate(), null, null,
                node.getRemark(), node.getOperatorName(), node.getProcessedAt()
        );
    }

}
