package com.plating.erp.common.api.response;

public class CommonResponses {
    public record SuccessResponse(boolean success) {
    }

    public record DeleteResponse(boolean deleted, Long id) {
    }

    public record ReadStatusResponse(Long noticeId, Integer readStatus) {
    }

    public record TokenResponse(String accessToken, Integer expiresIn) {
    }

    public record ResetPasswordResponse(Long userId, String newPassword, boolean needChange, boolean sessionsInvalidated, boolean emailSent, boolean smsSent) {
    }

    public record MenuItemResponse(Long id, String menuName, String path) {
    }

    public record TodoItemResponse(Long todoId, String title) {
    }

    public record ExportResponse(boolean exported, String fileName, Object filters) {
    }

    public record UserRoleBindResponse(Long userId, int bindCount, boolean success) {
    }
}
