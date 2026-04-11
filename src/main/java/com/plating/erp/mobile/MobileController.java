package com.plating.erp.mobile;

import com.plating.erp.auth.vo.AuthVo;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.common.security.JwtTokenService;
import com.plating.erp.common.security.RefreshTokenService;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.service.UserService;
import com.plating.erp.message.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mobile")
public class MobileController {
    private final MessageService messageService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenService jwtTokenService;
    private final UserService userService;

    public MobileController(MessageService messageService, RefreshTokenService refreshTokenService, JwtTokenService jwtTokenService, UserService userService) {
        this.messageService = messageService;
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenService = jwtTokenService;
        this.userService = userService;
    }

    @GetMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> myMessages(@RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "20") Integer pageSize,
                                     @RequestParam(required = false) Integer readStatus) {
        var page = messageService.myNotices(pageNum, pageSize, SecurityUtils.currentUser().userId(), readStatus);
        return ApiResponse.ok(new PageResult<>(page.getRecords(), page.getTotal()));
    }

    @PatchMapping("/messages/{noticeId}/read")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "移动端消息", operateType = "READ", bizModule = "mobile_notice", fieldName = "read_status")
    public ApiResponse<CommonResponses.ReadStatusResponse> readMessage(@PathVariable Long noticeId) {
        messageService.readNotice(noticeId, SecurityUtils.currentUser().userId());
        return ApiResponse.ok(new CommonResponses.ReadStatusResponse(noticeId, 1));
    }

    @GetMapping("/todos")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CommonResponses.TodoItemResponse>> myTodos() {
        return ApiResponse.ok(List.of(new CommonResponses.TodoItemResponse(90001L, "审批工单")));
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<?> refresh(@Valid @RequestBody AuthVo.RefreshReq req) {
        String session = refreshTokenService.validate(req.refreshToken());
        if (session == null) {
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        String[] parts = session.split(":");
        Long userId = Long.parseLong(parts[0]);
        Long tenantId = Long.parseLong(parts[1]);
        UserEntity user = userService.getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        List<String> roles = "system".equalsIgnoreCase(user.getUsername()) ? List.of("system") : List.of("TENANT_ADMIN");
        String accessToken = jwtTokenService.createToken(userId, tenantId, user.getUsername(), roles);
        return ApiResponse.ok(new CommonResponses.TokenResponse(accessToken, 7200));
    }
}
