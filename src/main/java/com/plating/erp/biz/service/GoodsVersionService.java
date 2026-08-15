package com.plating.erp.biz.service;

import com.plating.erp.biz.vo.GoodsVersionVo;

import java.util.List;

/**
 * 版本管理服务
 *
 * @author Plating ERP Team
 */
public interface GoodsVersionService {

    /**
     * 获取开单版本历史
     */
    List<GoodsVersionVo.OrderVersionHistoryVo> getOrderVersions(Long orderId, Long tenantId);

    /**
     * 获取节点版本历史
     */
    List<GoodsVersionVo.NodeVersionHistoryVo> getNodeVersions(Long nodeId, Long tenantId);

    /**
     * 获取变更日志
     */
    List<GoodsVersionVo.ChangeLogVo> getChangeLogs(Long orderId, Long tenantId);

    /**
     * 获取变更日志（按节点）
     */
    List<GoodsVersionVo.ChangeLogVo> getChangeLogsByNode(Long nodeId, Long tenantId);

    /**
     * 保存开单版本快照（含完整items JSON）
     */
    void takeSnapshot(Long orderId, Long tenantId, Long changedBy);

    /**
     * 保存节点版本快照
     */
    void takeNodeSnapshot(Long nodeId, Long tenantId, Long changedBy);
}
