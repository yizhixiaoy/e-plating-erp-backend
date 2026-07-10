package com.plating.erp.biz.service.impl;

import com.plating.erp.biz.service.GoodsImageAiService;
import com.plating.erp.biz.service.GoodsImageAiService.CrossCheckResult;
import com.plating.erp.biz.service.GoodsImageAiService.ImageMatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 图片AI服务实现
 * 负责将图片特征推送到AI服务，以及调用AI服务进行图片搜索和交叉比对
 *
 * @author Plating ERP Team
 */
@Slf4j
@Service
public class GoodsImageAiServiceImpl implements GoodsImageAiService {

    @Value("${app.ai.service-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    @Value("${app.ai.internal-api-key:}")
    private String aiServiceApiKey;

    private final RestTemplate restTemplate;

    public GoodsImageAiServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Async
    public void pushImageFeaturesToAiService(List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }
        try {
            // 调用AI服务批量注册图片特征
            Map<String, Object> request = Map.of(
                    "image_ids", imageIds
            );
            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServiceBaseUrl + "/api/ai/goods/register-features",
                    HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("图片特征推送成功: count={}", imageIds.size());
            } else {
                log.warn("图片特征推送失败: status={}, body={}",
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("图片特征推送异常: count={}", imageIds.size(), e);
            // 推送失败不影响主流程
        }
    }

    @Override
    public List<ImageMatchResult> searchSimilarImages(String queryImageUrl, Double threshold, Long tenantId) {
        try {
            Map<String, Object> request = Map.of(
                    "image_url", queryImageUrl,
                    "threshold", threshold != null ? threshold : 0.7,
                    "tenant_id", tenantId
            );
            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServiceBaseUrl + "/api/ai/goods/search-image",
                    HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> matches = (List<Map<String, Object>>) body.get("matches");
                if (matches != null) {
                    return matches.stream().map(m -> new ImageMatchResult(
                            ((Number) m.get("item_id")).longValue(),
                            (String) m.get("order_no"),
                            (String) m.get("item_name"),
                            ((Number) m.get("similarity")).doubleValue(),
                            (String) m.get("thumbnail_url")
                    )).toList();
                }
            }
        } catch (Exception e) {
            log.error("图片相似度搜索异常", e);
        }
        return Collections.emptyList();
    }

    @Override
    public CrossCheckResult crossCheck(List<String> nodeImageUrls, Long itemId, Long tenantId) {
        try {
            Map<String, Object> request = Map.of(
                    "image_urls", nodeImageUrls,
                    "item_id", itemId,
                    "tenant_id", tenantId
            );
            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServiceBaseUrl + "/api/ai/goods/cross-check",
                    HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                return new CrossCheckResult(
                        Boolean.TRUE.equals(body.get("matched")),
                        ((Number) body.get("max_similarity")).doubleValue(),
                        ((Number) body.get("matched_item_id")).longValue(),
                        (String) body.get("matched_item_name")
                );
            }
        } catch (Exception e) {
            log.error("交叉比对异常", e);
        }
        return new CrossCheckResult(false, 0.0, null, null);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (aiServiceApiKey != null && !aiServiceApiKey.isBlank()) {
            headers.set("X-Internal-Api-Key", aiServiceApiKey);
        }
        return headers;
    }
}
