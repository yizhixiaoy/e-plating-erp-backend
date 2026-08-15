package com.plating.erp.biz.vo;

import java.util.List;

/**
 * 版本历史和变更日志相关 VO
 *
 * @author Plating ERP Team
 */
public class GoodsVersionVo {

    /**
     * 开单版本历史响应
     */
    public record OrderVersionHistoryVo(
            Long id,
            Integer versionNo,
            String status,
            String changeSummary,
            Long changedBy,
            String changedByName,
            java.time.LocalDateTime changedAt,
            Object fullSnapshot
    ) {
    }

    /**
     * 节点版本历史响应
     */
    public record NodeVersionHistoryVo(
            Long id,
            Integer versionNo,
            String status,
            String changeSummary,
            Long changedBy,
            String changedByName,
            java.time.LocalDateTime changedAt,
            Object fullSnapshot
    ) {
    }

    /**
     * 变更日志响应
     */
    public record ChangeLogVo(
            Long id,
            String changeType,
            String changeTypeName,
            Long nodeId,
            String fieldName,
            Object oldValue,
            Object newValue,
            Long changedBy,
            String changedByName,
            String remark,
            java.time.LocalDateTime changedAt
    ) {
    }
}
