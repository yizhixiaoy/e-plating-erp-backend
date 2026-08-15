package com.plating.erp.biz.service;

import java.util.List;

/**
 * 图片AI服务 - 负责将图片特征推送到AI服务
 *
 * @author Plating ERP Team
 */
public interface GoodsImageAiService {

    /**
     * 推送货物图片特征到AI服务（异步）
     *
     * @param imageIds 图片ID列表
     */
    void pushImageFeaturesToAiService(List<Long> imageIds);

    /**
     * 从AI服务获取图片相似度搜索结果
     *
     * @param queryImageUrl 查询图片URL
     * @param threshold 相似度阈值
     * @param tenantId 租户ID
     * @return 匹配结果列表
     */
    List<ImageMatchResult> searchSimilarImages(String queryImageUrl, Double threshold, Long tenantId);

    /**
     * 交叉比对：加工节点图片与原样品比对
     *
     * @param nodeImageUrls 节点图片URL列表
     * @param itemId 货物明细ID
     * @param tenantId 租户ID
     * @return 比对结果
     */
    CrossCheckResult crossCheck(List<String> nodeImageUrls, Long itemId, Long tenantId);

    /**
     * 图片匹配结果
     */
    record ImageMatchResult(
            Long itemId,
            String orderNo,
            String itemName,
            Double similarity,
            String thumbnailUrl
    ) {
    }

    /**
     * 交叉比对结果
     */
    record CrossCheckResult(
            boolean matched,
            Double maxSimilarity,
            Long matchedItemId,
            String matchedItemName
    ) {
    }
}
