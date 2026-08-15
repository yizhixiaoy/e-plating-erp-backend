package com.plating.erp.biz.service;

import com.plating.erp.biz.vo.GoodsProcessVo;
import com.plating.erp.common.api.response.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 货物加工Service
 *
 * @author Plating ERP Team
 */
public interface GoodsProcessService {

    /**
     * 录入加工数据
     */
    GoodsProcessVo.ProcessRecordVo submitProcess(GoodsProcessVo.ProcessSubmitReq req);

    /**
     * 变更节点状态（状态机校验）
     */
    void changeNodeStatus(GoodsProcessVo.NodeStatusChangeReq req);

    /**
     * 回退节点到上一状态
     */
    void rollbackNode(GoodsProcessVo.NodeRollbackReq req);

    /**
     * 查询开单的所有加工节点
     */
    List<GoodsProcessVo.ProcessRecordVo> getNodesByOrderId(Long orderId, Long tenantId);

    /**
     * 查询节点加工记录
     */
    GoodsProcessVo.ProcessRecordVo getRecordByNodeId(Long nodeId, Long tenantId);

    /**
     * 多条件搜索货物
     */
    PageResult<GoodsProcessVo.GoodsSearchResultVo> searchGoods(GoodsProcessVo.GoodsSearchReq req,
                                                                Long tenantId);

    /**
     * 图片搜索货物
     */
    List<GoodsProcessVo.ImageSearchResultVo> searchByImage(String imageUrl, Double threshold,
                                                            Long tenantId);

    /**
     * 获取开单的加工记录汇总
     */
    List<GoodsProcessVo.ProcessRecordVo> getProcessRecordsByOrderId(Long orderId, Long tenantId);

    /**
     * 批量获取货物明细业务数据（供AI服务调用）
     * 返回 {itemId, orderNo, itemName, customerName}
     */
    List<Map<String, Object>> getBusinessData(List<Long> itemIds, Long tenantId);
}
