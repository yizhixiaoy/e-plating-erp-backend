package com.plating.erp.mobile.controller;

import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.message.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mobile")
public class MobileController {
    private final MessageService messageService;

    public MobileController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/messages")
    public ApiResponse<List<?>> getMessages(CurrentUser user) {
        // 调用消息服务获取用户消息列表
        var result = messageService.myNotices(1, 100, user.userId(), null);
        return ApiResponse.ok(result.records());
    }

    @PatchMapping("/messages/{noticeId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long noticeId, CurrentUser user) {
        // 标记消息为已读
        messageService.readNotice(noticeId, user.userId());
        return ApiResponse.ok(null);
    }

    @GetMapping("/todos")
    public ApiResponse<List<?>> getTodos(CurrentUser user) {
        // 第一阶段：返回空列表或模拟数据
        return ApiResponse.ok(List.of());
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<?> refreshToken(@RequestBody java.util.Map<String, String> body) {
        // Token刷新逻辑，复用AuthService
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ApiResponse.error(400, "刷新令牌不能为空");
        }
        // 实际实现应调用AuthService.refreshToken
        return ApiResponse.ok(java.util.Map.of(
            "accessToken", "new_token_placeholder",
            "expiresIn", 7200
        ));
    }
}
