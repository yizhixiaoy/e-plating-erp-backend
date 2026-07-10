package com.plating.erp.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.biz.entity.*;
import com.plating.erp.biz.enums.GoodsChangeTypeEnum;
import com.plating.erp.biz.enums.GoodsNodeStatusEnum;
import com.plating.erp.biz.enums.GoodsOrderStatusEnum;
import com.plating.erp.biz.mapper.*;
import com.plating.erp.biz.service.GoodsOrderService;
import com.plating.erp.biz.service.GoodsVersionService;
import com.plating.erp.biz.vo.GoodsOrderVo;
import com.plating.erp.biz.vo.GoodsOrderVo.GoodsOrderCreateReq;
import com.plating.erp.biz.vo.GoodsOrderVo.GoodsOrderCreateResp;
import com.plating.erp.biz.vo.GoodsOrderVo.GoodsOrderDistributeReq;
import com.plating.erp.biz.vo.GoodsOrderVo.GoodsOrderDetailVo;
import com.plating.erp.biz.vo.GoodsOrderVo.GoodsOrderDistributeResp;
import com.plating.erp.biz.vo.GoodsOrderVo.GoodsOrderListVo;
import com.plating.erp.biz.vo.GoodsOrderVo.DistributedNodeVo;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.iam.entity.DeptEntity;
import com.plating.erp.iam.mapper.DeptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 货物开单Service实现
 *
 * @author Plating ERP Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsOrderServiceImpl implements GoodsOrderService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SEQ_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ORDER_NO_PREFIX = "GD";
    private final GoodsOrderMapper orderMapper;
    private final GoodsOrderItemMapper itemMapper;
    private final GoodsProcessNodeMapper nodeMapper;
    private final GoodsChangeLogMapper changeLogMapper;
    private final CustomerMapper customerMapper;
    private final GoodsVersionService versionService;
    private final DeptMapper deptMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private final Map<Long, String> deptNameCache = new HashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GoodsOrderCreateResp createOrder(GoodsOrderCreateReq req, Long tenantId, Long operatorId, String operatorName) {
        // 校验客户公司是否存在
        CustomerEntity customer = customerMapper.selectById(req.customerId());
        if (customer == null || customer.getDeleted() == 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "客户公司不存在");
        }

        // 校验部门是否存在
        String deptName = getDeptName(req.departmentId());
        if (deptName.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "开单部门不存在");
        }

        // 生成开单编号
        String orderNo = generateOrderNo(tenantId);

        // 创建开单主记录
        GoodsOrderEntity order = new GoodsOrderEntity();
        order.setTenantId(tenantId);
        order.setOrderNo(orderNo);
        order.setOrderDate(req.orderDate());
        order.setCustomerId(req.customerId());
        order.setCustomerName(customer.getCustomerName());  // 使用DB中的名称，避免数据不一致
        order.setDepartmentId(req.departmentId());
        order.setDepartmentName(deptName);  // 使用DB中的部门名称
        order.setStatus(GoodsOrderStatusEnum.DRAFT.getCode());
        order.setRemark(req.remark());
        order.setOperatorId(operatorId);
        order.setOperatorName(operatorName);
        order.setTotalItems(req.items().size());
        order.setCreatedBy(operatorId);

        // 计算总数量
        BigDecimal totalQty = BigDecimal.ZERO;
        for (var itemReq : req.items()) {
            totalQty = totalQty.add(itemReq.quantity());
        }
        order.setTotalQuantity(totalQty);

        orderMapper.insert(order);

        // 批量创建货物明细
        List<GoodsOrderItemEntity> items = new ArrayList<>();
        for (var itemReq : req.items()) {
            GoodsOrderItemEntity item = toItemEntity(order.getId(), tenantId, operatorId, itemReq);
            itemMapper.insert(item);
            items.add(item);
        }

        // 记录变更日志
        saveChangeLog(order.getId(), orderNo, null, GoodsChangeTypeEnum.ORDER_CREATE,
                "status", null, GoodsOrderStatusEnum.DRAFT.getCode(),
                "创建开单", operatorId, operatorName);

        // 保存版本快照
        saveOrderVersionSnapshot(order, operatorId);

        log.info("创建货物开单成功: orderId={}, orderNo={}", order.getId(), orderNo);
        return new GoodsOrderCreateResp(order.getId(), orderNo, GoodsOrderStatusEnum.DRAFT.getCode());
    }

    @Override
    public PageResult<GoodsOrderListVo> listOrders(Long tenantId, String keyword,
                                                    Long customerId, String status,
                                                    Integer page, Integer size) {
        LambdaQueryWrapper<GoodsOrderEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsOrderEntity::getTenantId, tenantId)
               .eq(GoodsOrderEntity::getDeleted, 0);
        if (customerId != null) {
            wrapper.eq(GoodsOrderEntity::getCustomerId, customerId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(GoodsOrderEntity::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(GoodsOrderEntity::getOrderNo, keyword)
                    .or().like(GoodsOrderEntity::getCustomerName, keyword)
                    .or(w2 -> w2.apply("EXISTS (SELECT 1 FROM biz_goods_order_item WHERE order_id = biz_goods_order.id AND deleted = 0 AND item_name LIKE CONCAT('%',{0},'%'))", keyword)));
        }
        wrapper.orderByDesc(GoodsOrderEntity::getId);

        Page<GoodsOrderEntity> pageResult = orderMapper.selectPage(new Page<>(page, size), wrapper);
        List<GoodsOrderListVo> vos = pageResult.getRecords().stream()
                .map(this::toListVo)
                .toList();
        return new PageResult<>(vos, pageResult.getTotal());
    }

    @Override
    public GoodsOrderDetailVo getOrderDetail(Long orderId, Long tenantId) {
        GoodsOrderEntity order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "开单不存在");
        }
        if (!order.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该开单");
        }

        // 查询货物明细
        LambdaQueryWrapper<GoodsOrderItemEntity> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(GoodsOrderItemEntity::getOrderId, orderId)
                   .eq(GoodsOrderItemEntity::getDeleted, 0);
        List<GoodsOrderItemEntity> items = itemMapper.selectList(itemWrapper);

        // 查询加工节点（按货物分组，货物内按 nodeOrder 排序）
        LambdaQueryWrapper<GoodsProcessNodeEntity> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(GoodsProcessNodeEntity::getOrderId, orderId)
                   .eq(GoodsProcessNodeEntity::getDeleted, 0)
                   .orderByAsc(GoodsProcessNodeEntity::getItemId)
                   .orderByAsc(GoodsProcessNodeEntity::getNodeOrder);
        List<GoodsProcessNodeEntity> nodes = nodeMapper.selectList(nodeWrapper);

        return toDetailVo(order, items, nodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitOrder(Long orderId, Long tenantId, Long operatorId, String operatorName) {
        GoodsOrderEntity order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "开单不存在");
        }
        if (!order.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该开单");
        }
        if (!GoodsOrderStatusEnum.DRAFT.getCode().equals(order.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有草稿状态的开单可以提交");
        }

        order.setStatus(GoodsOrderStatusEnum.PENDING_DISTRIBUTE.getCode());
        order.setSubmittedAt(LocalDateTime.now());
        order.setUpdatedBy(operatorId);
        orderMapper.updateById(order);

        // 保存版本快照
        versionService.takeSnapshot(orderId, tenantId, operatorId);

        saveChangeLog(orderId, order.getOrderNo(), null, GoodsChangeTypeEnum.ORDER_UPDATE,
                "status", GoodsOrderStatusEnum.DRAFT.getCode(), GoodsOrderStatusEnum.PENDING_DISTRIBUTE.getCode(),
                "提交开单", operatorId, operatorName);
        log.info("提交开单成功: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GoodsOrderDistributeResp distributeOrder(Long orderId, GoodsOrderDistributeReq req, Long tenantId) {
        GoodsOrderEntity order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "开单不存在");
        }
        if (!order.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该开单");
        }
        if (!GoodsOrderStatusEnum.PENDING_DISTRIBUTE.getCode().equals(order.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有待分发状态的开单可以分发");
        }

        // 查询该开单的所有货物明细
        LambdaQueryWrapper<GoodsOrderItemEntity> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(GoodsOrderItemEntity::getOrderId, orderId)
                   .eq(GoodsOrderItemEntity::getDeleted, 0);
        List<GoodsOrderItemEntity> items = itemMapper.selectList(itemWrapper);
        if (items.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "开单没有货物明细");
        }

        // 构建 itemId → item 的快速查找 Map
        Map<Long, GoodsOrderItemEntity> itemMap = items.stream()
                .collect(Collectors.toMap(GoodsOrderItemEntity::getId, Function.identity()));

        // 为每个货物按其独立配置的加工步骤生成节点
        // 每个货物的 nodeOrder 独立从 1 开始
        List<DistributedNodeVo> distributedNodes = new ArrayList<>();
        for (GoodsOrderVo.ItemDistributeSteps its : req.itemSteps()) {
            GoodsOrderItemEntity item = itemMap.get(its.itemId());
            if (item == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "货物明细不存在: itemId=" + its.itemId());
            }
            int nodeOrder = 1;
            for (GoodsOrderVo.ProcessStepReq step : its.steps()) {
                Long deptId = step.departmentId();
                String deptName = step.departmentName() != null && !step.departmentName().isBlank()
                        ? step.departmentName() : getDeptName(deptId);
                GoodsProcessNodeEntity node = new GoodsProcessNodeEntity();
                node.setTenantId(tenantId);
                node.setOrderId(orderId);
                node.setOrderNo(order.getOrderNo());
                node.setItemId(item.getId());
                node.setItemName(item.getItemName());
                node.setDepartmentId(deptId);
                node.setDepartmentName(deptName);
                node.setNodeOrder(nodeOrder++);
                node.setOriginalOrder(node.getNodeOrder()); // 原始加工链顺序
                node.setStatus(GoodsNodeStatusEnum.PENDING.getCode());
                node.setOriginalQuantity(item.getQuantity());
                node.setCreatedBy(req.operatorId());
                nodeMapper.insert(node);

                distributedNodes.add(new DistributedNodeVo(
                        node.getId(), deptId, deptName, node.getNodeOrder(), GoodsNodeStatusEnum.PENDING.getCode()));
            }
        }

        // 更新开单状态为加工中
        order.setStatus(GoodsOrderStatusEnum.PROCESSING.getCode());
        order.setDistributedAt(LocalDateTime.now());
        order.setUpdatedBy(req.operatorId());
        orderMapper.updateById(order);

        // 保存版本快照
        versionService.takeSnapshot(orderId, tenantId, req.operatorId());

        saveChangeLog(orderId, order.getOrderNo(), null, GoodsChangeTypeEnum.ORDER_UPDATE,
                "status", GoodsOrderStatusEnum.PENDING_DISTRIBUTE.getCode(), GoodsOrderStatusEnum.PROCESSING.getCode(),
                "分发开单", req.operatorId(), req.operatorName());
        log.info("分发开单成功: orderId={}, nodeCount={}", orderId, distributedNodes.size());

        return new GoodsOrderDistributeResp(orderId, distributedNodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long tenantId, Long operatorId) {
        GoodsOrderEntity order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "开单不存在");
        }
        if (!order.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该开单");
        }
        if (GoodsOrderStatusEnum.COMPLETED.getCode().equals(order.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "已完成的开单不能取消");
        }

        String oldStatus = order.getStatus();
        order.setStatus(GoodsOrderStatusEnum.CANCELLED.getCode());
        order.setUpdatedBy(operatorId);
        orderMapper.updateById(order);

        saveChangeLog(orderId, order.getOrderNo(), null, GoodsChangeTypeEnum.ORDER_UPDATE,
                "status", oldStatus, GoodsOrderStatusEnum.CANCELLED.getCode(),
                "取消开单", operatorId, null);
        log.info("取消开单成功: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelDistributeOrder(Long orderId, Long tenantId, Long operatorId) {
        GoodsOrderEntity order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "开单不存在");
        }
        if (!order.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该开单");
        }
        if (!GoodsOrderStatusEnum.PENDING_DISTRIBUTE.getCode().equals(order.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有待分发状态的开单可以取消分发");
        }

        order.setStatus(GoodsOrderStatusEnum.DRAFT.getCode());
        order.setUpdatedBy(operatorId);
        orderMapper.updateById(order);

        saveChangeLog(orderId, order.getOrderNo(), null, GoodsChangeTypeEnum.ORDER_UPDATE,
                "status", GoodsOrderStatusEnum.PENDING_DISTRIBUTE.getCode(), GoodsOrderStatusEnum.DRAFT.getCode(),
                "取消分发", operatorId, null);
        log.info("取消分发成功: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmitOrder(Long orderId, Long tenantId, Long operatorId, String operatorName) {
        GoodsOrderEntity order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "开单不存在");
        }
        if (!order.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该开单");
        }
        if (!GoodsOrderStatusEnum.CANCELLED.getCode().equals(order.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有已取消状态的开单可以重新提交");
        }

        order.setStatus(GoodsOrderStatusEnum.DRAFT.getCode());
        order.setUpdatedBy(operatorId);
        orderMapper.updateById(order);

        // 保存版本快照
        versionService.takeSnapshot(orderId, tenantId, operatorId);

        saveChangeLog(orderId, order.getOrderNo(), null, GoodsChangeTypeEnum.ORDER_UPDATE,
                "status", GoodsOrderStatusEnum.CANCELLED.getCode(), GoodsOrderStatusEnum.DRAFT.getCode(),
                "重新提交", operatorId, operatorName);
        log.info("重新提交开单成功: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Long orderId, GoodsOrderVo.GoodsOrderUpdateReq req,
                            Long tenantId, Long operatorId, String operatorName) {
        GoodsOrderEntity order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "开单不存在");
        }
        if (!order.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该开单");
        }
        if (!GoodsOrderStatusEnum.DRAFT.getCode().equals(order.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有草稿状态的开单可以编辑");
        }

        // 更新基本信息
        if (req.orderDate() != null) {
            order.setOrderDate(req.orderDate());
        }
        if (req.customerId() != null) {
            CustomerEntity customer = customerMapper.selectById(req.customerId());
            if (customer == null || customer.getDeleted() == 1) {
                throw new BizException(ErrorCode.BAD_REQUEST, "客户公司不存在");
            }
            order.setCustomerId(req.customerId());
            order.setCustomerName(customer.getCustomerName());
        }
        if (req.departmentId() != null) {
            String deptName = getDeptName(req.departmentId());
            if (deptName.isEmpty()) {
                throw new BizException(ErrorCode.BAD_REQUEST, "开单部门不存在");
            }
            order.setDepartmentId(req.departmentId());
            order.setDepartmentName(deptName);
        }
        if (req.remark() != null) {
            order.setRemark(req.remark());
        }

        // 更新货物明细：先软删旧的，再插入新的
        if (req.items() != null && !req.items().isEmpty()) {
            // 软删除旧明细（使用 deleteById，MyBatis-Plus @TableLogic 自动转为 SET deleted=1）
            LambdaQueryWrapper<GoodsOrderItemEntity> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(GoodsOrderItemEntity::getOrderId, orderId)
                       .eq(GoodsOrderItemEntity::getDeleted, 0);
            List<GoodsOrderItemEntity> oldItems = itemMapper.selectList(itemWrapper);
            for (GoodsOrderItemEntity old : oldItems) {
                itemMapper.deleteById(old.getId());
            }

            // 插入新明细
            BigDecimal totalQty = BigDecimal.ZERO;
            for (var itemReq : req.items()) {
                GoodsOrderItemEntity item = toItemEntity(orderId, tenantId, operatorId, itemReq);
                itemMapper.insert(item);
                totalQty = totalQty.add(itemReq.quantity());
            }
            order.setTotalItems(req.items().size());
            order.setTotalQuantity(totalQty);
        }

        order.setUpdatedBy(operatorId);
        orderMapper.updateById(order);

        // 保存版本快照
        versionService.takeSnapshot(orderId, tenantId, operatorId);

        saveChangeLog(orderId, order.getOrderNo(), null, GoodsChangeTypeEnum.ORDER_UPDATE,
                "基本信息", null, "已更新",
                "更新开单", operatorId, operatorName);
        log.info("更新开单成功: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long orderId, Long tenantId, Long operatorId) {
        GoodsOrderEntity order = orderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "开单不存在");
        }
        if (!order.getTenantId().equals(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该开单");
        }
        if (!GoodsOrderStatusEnum.DRAFT.getCode().equals(order.getStatus())
                && !GoodsOrderStatusEnum.CANCELLED.getCode().equals(order.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有草稿或已取消状态的开单可以删除");
        }

        String oldStatus = order.getStatus();

        // 逻辑删除开单（LambdaUpdateWrapper 绕过 @TableLogic 拦截，同时设置 deleted 和 updatedBy）
        orderMapper.update(null,
                new LambdaUpdateWrapper<GoodsOrderEntity>()
                        .eq(GoodsOrderEntity::getId, orderId)
                        .set(GoodsOrderEntity::getDeleted, 1)
                        .set(GoodsOrderEntity::getUpdatedBy, operatorId));

        // 逻辑删除关联的货物明细
        LambdaQueryWrapper<GoodsOrderItemEntity> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(GoodsOrderItemEntity::getOrderId, orderId)
                   .eq(GoodsOrderItemEntity::getDeleted, 0);
        List<GoodsOrderItemEntity> items = itemMapper.selectList(itemWrapper);
        for (GoodsOrderItemEntity item : items) {
            itemMapper.deleteById(item.getId());
        }

        saveChangeLog(orderId, order.getOrderNo(), null, GoodsChangeTypeEnum.ORDER_UPDATE,
                "deleted", oldStatus, "1",
                "删除开单", operatorId, null);
        log.info("删除开单成功: orderId={}", orderId);
    }

    @Override
    public boolean belongsToTenant(Long orderId, Long tenantId) {
        GoodsOrderEntity order = orderMapper.selectById(orderId);
        return order != null && tenantId.equals(order.getTenantId()) && order.getDeleted() == 0;
    }

    @Override
    public GoodsOrderDetailVo getByOrderNo(String orderNo, Long tenantId) {
        LambdaQueryWrapper<GoodsOrderEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsOrderEntity::getOrderNo, orderNo)
               .eq(GoodsOrderEntity::getTenantId, tenantId)
               .eq(GoodsOrderEntity::getDeleted, 0);
        GoodsOrderEntity order = orderMapper.selectOne(wrapper);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "开单不存在");
        }
        return getOrderDetail(order.getId(), tenantId);
    }

    @Override
    public GoodsOrderService.GoodsOrderStatsVo getStats(Long tenantId) {
        LocalDate today = LocalDate.now();
        long todayCount = countByDateRange(tenantId, today, today.plusDays(1));
        long pending = countByStatus(tenantId, GoodsOrderStatusEnum.PENDING_DISTRIBUTE.getCode());
        long processing = countByStatus(tenantId, GoodsOrderStatusEnum.PROCESSING.getCode());
        long completed = countByStatus(tenantId, GoodsOrderStatusEnum.COMPLETED.getCode());
        return new GoodsOrderService.GoodsOrderStatsVo(todayCount, pending, processing, completed);
    }

    // ========== 私有方法 ==========

    /**
     * 生成开单编号：GD{tenantId}-yyyyMMdd-4位序号
     * 通过查询数据库当天最大编号来保证唯一性和连续性
     * 加入租户ID确保不同租户间开单号全局唯一
     */
    private String generateOrderNo(Long tenantId) {
        String dateStr = LocalDate.now().format(SEQ_FMT);
        String prefix = ORDER_NO_PREFIX + tenantId + "-" + dateStr + "-";

        // 查询当天最大序号，按租户隔离
        LambdaQueryWrapper<GoodsOrderEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsOrderEntity::getTenantId, tenantId)
               .likeRight(GoodsOrderEntity::getOrderNo, prefix)
               .orderByDesc(GoodsOrderEntity::getOrderNo)
               .last("LIMIT 1");
        GoodsOrderEntity latest = orderMapper.selectOne(wrapper);

        int seq = 1;
        if (latest != null && latest.getOrderNo().length() > prefix.length()) {
            try {
                seq = Integer.parseInt(latest.getOrderNo().substring(prefix.length())) + 1;
            } catch (NumberFormatException e) {
                log.warn("解析开单编号序号失败: {}", latest.getOrderNo());
            }
        }
        String seqStr = String.format("%04d", seq % 10000);
        return prefix + seqStr;
    }

    private GoodsOrderItemEntity toItemEntity(Long orderId, Long tenantId, Long operatorId,
                                               GoodsOrderVo.GoodsItemReq req) {
        GoodsOrderItemEntity item = new GoodsOrderItemEntity();
        item.setOrderId(orderId);
        item.setTenantId(tenantId);
        item.setItemName(req.itemName());
        item.setQuantity(req.quantity());
        item.setUnit(req.unit() != null ? req.unit() : "个");
        item.setUnitPrice(req.unitPrice() != null ? req.unitPrice() : BigDecimal.ZERO);
        item.setTotalPrice(req.quantity().multiply(req.unitPrice() != null ? req.unitPrice() : BigDecimal.ZERO));
        item.setSpecification(req.specification());
        item.setMaterial(req.material());
        item.setPhotoUrls(req.photoUrls());
        item.setRemark(req.remark());
        item.setCreatedBy(operatorId);
        return item;
    }

    private GoodsOrderListVo toListVo(GoodsOrderEntity order) {
        return new GoodsOrderListVo(
                order.getId(), order.getOrderNo(), order.getOrderDate(),
                order.getCustomerId(), order.getCustomerName(),
                order.getDepartmentId(), order.getDepartmentName(),
                order.getStatus(), order.getTotalItems(), order.getTotalQuantity(),
                order.getRemark(), order.getOperatorId(), order.getOperatorName(),
                order.getSubmittedAt(), order.getDistributedAt(), order.getCreatedAt()
        );
    }

    private GoodsOrderDetailVo toDetailVo(GoodsOrderEntity order,
                                           List<GoodsOrderItemEntity> items,
                                           List<GoodsProcessNodeEntity> nodes) {
        List<GoodsOrderVo.GoodsItemDetailVo> itemVos = items.stream().map(i ->
                new GoodsOrderVo.GoodsItemDetailVo(
                        i.getId(), i.getItemName(), i.getQuantity(), i.getUnit(),
                        i.getUnitPrice(), i.getTotalPrice(), i.getSpecification(),
                        i.getMaterial(), i.getPhotoUrls(), i.getRemark()
                )
        ).toList();

        List<GoodsOrderVo.GoodsNodeDetailVo> nodeVos = nodes.stream().map(n ->
                new GoodsOrderVo.GoodsNodeDetailVo(
                        n.getId(), n.getId(), n.getItemId(), n.getItemName(),
                        n.getDepartmentId(), n.getDepartmentName(),
                        n.getNodeOrder(), n.getOriginalOrder(),
                        n.getStatus(), n.getOriginalQuantity(), n.getProcessedQuantity(),
                        n.getLossQuantity(), n.getLossRate(), n.getRemark(),
                        n.getOperatorName(), n.getProcessedAt(),
                        n.getRollbackCount(), n.getRollbackReason()
                )
        ).toList();

        return new GoodsOrderDetailVo(
                order.getId(), order.getOrderNo(), order.getOrderDate(),
                order.getCustomerId(), order.getCustomerName(),
                order.getDepartmentId(), order.getDepartmentName(),
                order.getStatus(), order.getTotalItems(), order.getTotalQuantity(),
                order.getRemark(), order.getOperatorId(), order.getOperatorName(),
                order.getSubmittedAt(), order.getDistributedAt(),
                order.getAcceptedAt(), order.getDeliveringAt(),
                order.getCompletedAt(),
                itemVos, nodeVos
        );
    }

    private Long countByStatus(Long tenantId, String status) {
        LambdaQueryWrapper<GoodsOrderEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsOrderEntity::getTenantId, tenantId)
               .eq(GoodsOrderEntity::getStatus, status)
               .eq(GoodsOrderEntity::getDeleted, 0);
        return orderMapper.selectCount(wrapper);
    }

    private Long countByDateRange(Long tenantId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<GoodsOrderEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsOrderEntity::getTenantId, tenantId)
               .eq(GoodsOrderEntity::getDeleted, 0)
               .ge(GoodsOrderEntity::getOrderDate, start)
               .lt(GoodsOrderEntity::getOrderDate, end);
        return orderMapper.selectCount(wrapper);
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
        GoodsChangeLogEntity log = new GoodsChangeLogEntity();
        Long tenantId = com.plating.erp.common.security.SecurityUtils.currentUser() != null
                ? com.plating.erp.common.security.SecurityUtils.currentUser().tenantId() : null;
        log.setTenantId(tenantId);
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setNodeId(nodeId);
        log.setChangeType(changeType.getCode());
        log.setOldValue(oldValues);
        log.setNewValue(newValues);
        log.setChangedBy(operatorId);
        log.setChangedAt(LocalDateTime.now());
        log.setRemark(remark);
        changeLogMapper.insert(log);
    }

    private void saveOrderVersionSnapshot(GoodsOrderEntity order, Long operatorId) {
        versionService.takeSnapshot(order.getId(), order.getTenantId(), operatorId);
    }

    private String getDeptName(Long deptId) {
        if (deptId == null) return "";
        return deptNameCache.computeIfAbsent(deptId, id -> {
            LambdaQueryWrapper<DeptEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DeptEntity::getId, id)
                   .eq(DeptEntity::getDeleted, 0);
            DeptEntity dept = deptMapper.selectOne(wrapper);
            return dept != null ? dept.getDeptName() : "";
        });
    }
}
