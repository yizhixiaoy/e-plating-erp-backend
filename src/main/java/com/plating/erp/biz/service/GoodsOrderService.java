package com.plating.erp.biz.service;

import com.plating.erp.biz.vo.GoodsOrderVo;
import com.plating.erp.common.api.response.PageResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * 货物开单Service
 *
 * @author Plating ERP Team
 */
public interface GoodsOrderService {

    /**
     * 创建货物开单（草稿）
     */
    GoodsOrderVo.GoodsOrderCreateResp createOrder(GoodsOrderVo.GoodsOrderCreateReq req,
                                                   Long tenantId, Long operatorId,
                                                   String operatorName);

    /**
     * 分页查询开单列表
     */
    PageResult<GoodsOrderVo.GoodsOrderListVo> listOrders(Long tenantId, String keyword,
                                                          Long customerId, String status,
                                                          Integer page, Integer size);

    /**
     * 获取开单详情（含明细和加工节点）
     */
    GoodsOrderVo.GoodsOrderDetailVo getOrderDetail(Long orderId, Long tenantId);

    /**
     * 提交开单（草稿 → 待分发）
     */
    void submitOrder(Long orderId, Long tenantId, Long operatorId, String operatorName);

    /**
     * 分发开单至加工部门
     */
    GoodsOrderVo.GoodsOrderDistributeResp distributeOrder(Long orderId,
                                                           GoodsOrderVo.GoodsOrderDistributeReq req,
                                                           Long tenantId);

    /**
     * 取消开单
     */
    void cancelOrder(Long orderId, Long tenantId, Long operatorId);

    /**
     * 取消分发（待分发 → 草稿）
     */
    void cancelDistributeOrder(Long orderId, Long tenantId, Long operatorId);

    /**
     * 重新提交（已取消 → 草稿）
     */
    void resubmitOrder(Long orderId, Long tenantId, Long operatorId, String operatorName);

    /**
     * 更新开单（仅草稿状态可编辑）
     */
    void updateOrder(Long orderId, GoodsOrderVo.GoodsOrderUpdateReq req,
                     Long tenantId, Long operatorId, String operatorName);

    /**
     * 删除开单（仅草稿状态可删除）
     */
    void deleteOrder(Long orderId, Long tenantId, Long operatorId);

    /**
     * 检查开单是否属于指定租户
     */
    boolean belongsToTenant(Long orderId, Long tenantId);

    /**
     * 根据开单编号查询
     */
    GoodsOrderVo.GoodsOrderDetailVo getByOrderNo(String orderNo, Long tenantId);

    /**
     * 获取开单统计数据
     */
    GoodsOrderStatsVo getStats(Long tenantId);

    /**
     * 开单统计响应
     */
    record GoodsOrderStatsVo(
            Long todayCount,
            Long pendingCount,
            Long processingCount,
            Long completedCount
    ) {
    }
}
