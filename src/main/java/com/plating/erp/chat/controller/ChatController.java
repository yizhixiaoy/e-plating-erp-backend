package com.plating.erp.chat.controller;

import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.chat.entity.ChatMessageEntity;
import com.plating.erp.chat.entity.ConversationEntity;
import com.plating.erp.chat.service.ChatService;
import com.plating.erp.chat.vo.ChatVo;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    public ChatController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    /** 我的会话列表 */
    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ChatVo.ConversationView>> myConversations(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "30") Integer pageSize,
            @RequestParam(required = false) String convType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean unreadOnly) {
        var u = SecurityUtils.currentUser();
        ChatVo.ConvQuery q = new ChatVo.ConvQuery(convType, keyword, unreadOnly);
        return ApiResponse.ok(chatService.myConversations(pageNum, pageSize, u.userId(), q));
    }

    /** 单个会话详情 */
    @GetMapping("/conversations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ChatVo.ConversationView> conversationDetail(@PathVariable Long id) {
        var u = SecurityUtils.currentUser();
        return ApiResponse.ok(chatService.conversationDetail(id, u.userId()));
    }

    /** 历史消息（按 id 倒序，beforeId 翻页） */
    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ChatVo.MessageView>> messages(
            @PathVariable Long id,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30") Integer limit) {
        var u = SecurityUtils.currentUser();
        return ApiResponse.ok(chatService.messages(id, u.userId(), beforeId, limit));
    }

    /** 发送消息 */
    @PostMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "聊天", operateType = "CREATE", bizModule = "chat", fieldName = "content")
    public ApiResponse<ChatMessageEntity> send(@Valid @RequestBody ChatVo.SendReq req) {
        var u = SecurityUtils.currentUser();
        return ApiResponse.ok(chatService.send(req, u.userId(), u.tenantId()));
    }

    /** 已读上报 */
    @PostMapping("/conversations/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CommonResponses.SuccessResponse> read(@Valid @RequestBody ChatVo.ReadReq req) {
        var u = SecurityUtils.currentUser();
        chatService.markRead(req, u.userId());
        return ApiResponse.ok(new CommonResponses.SuccessResponse(true));
    }

    /** 撤回消息 */
    @PostMapping("/messages/{id}/recall")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "聊天", operateType = "STATUS", bizModule = "chat", fieldName = "recalled")
    public ApiResponse<ChatMessageEntity> recall(@PathVariable Long id) {
        var u = SecurityUtils.currentUser();
        return ApiResponse.ok(chatService.recall(id, u.userId()));
    }

    /** 编辑消息（5 分钟内，仅本人，仅文本） */
    @PostMapping("/messages/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "聊天", operateType = "UPDATE", bizModule = "chat", fieldName = "content")
    public ApiResponse<ChatMessageEntity> edit(@PathVariable Long id,
                                               @Valid @RequestBody ChatVo.EditReq req) {
        var u = SecurityUtils.currentUser();
        return ApiResponse.ok(chatService.edit(id, req, u.userId()));
    }

    /** 切换置顶 */
    @PatchMapping("/conversations/pinned")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CommonResponses.SuccessResponse> togglePinned(@Valid @RequestBody ChatVo.ToggleReq req) {
        var u = SecurityUtils.currentUser();
        chatService.togglePinned(req, u.userId());
        return ApiResponse.ok(new CommonResponses.SuccessResponse(true));
    }

    /** 切换免打扰 */
    @PatchMapping("/conversations/muted")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CommonResponses.SuccessResponse> toggleMuted(@Valid @RequestBody ChatVo.ToggleReq req) {
        var u = SecurityUtils.currentUser();
        chatService.toggleMuted(req, u.userId());
        return ApiResponse.ok(new CommonResponses.SuccessResponse(true));
    }

    /** 创建/复用单聊 */
    @PostMapping("/conversations/single")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "聊天", operateType = "CREATE", bizModule = "chat", fieldName = "convType")
    public ApiResponse<ConversationEntity> createSingle(@Valid @RequestBody ChatVo.CreateSingleReq req) {
        var u = SecurityUtils.currentUser();
        return ApiResponse.ok(chatService.createSingle(req, u.userId(), u.tenantId()));
    }

    /** 创建群聊 */
    @PostMapping("/conversations/group")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "聊天", operateType = "CREATE", bizModule = "chat", fieldName = "name")
    public ApiResponse<ConversationEntity> createGroup(@Valid @RequestBody ChatVo.CreateGroupReq req) {
        var u = SecurityUtils.currentUser();
        return ApiResponse.ok(chatService.createGroup(req, u.userId(), u.tenantId()));
    }

    /** 全局未读统计（用于顶部导航小红点） */
    @GetMapping("/unread/summary")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ChatVo.UnreadSummary> unreadSummary() {
        var u = SecurityUtils.currentUser();
        return ApiResponse.ok(chatService.unreadSummary(u.userId()));
    }

    /** 删除/退出会话（逻辑删除成员关系，单聊双方均退出则会话可被清理） */
    @DeleteMapping("/conversations/{id}")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "聊天", operateType = "DELETE", bizModule = "chat", fieldName = "conversationId")
    public ApiResponse<CommonResponses.SuccessResponse> deleteConversation(@PathVariable Long id) {
        var u = SecurityUtils.currentUser();
        chatService.deleteConversation(id, u.userId());
        return ApiResponse.ok(new CommonResponses.SuccessResponse(true));
    }

    /** 聊天通讯录搜索（仅需登录即可所用，用于发起单聊/创建群聊） */
    @GetMapping("/contacts/search")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> searchContacts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") Integer limit) {
        var me = SecurityUtils.currentUser();
        if (me.tenantId() == null || keyword == null || keyword.isBlank()) {
            return ApiResponse.ok(List.of());
        }
        List<UserEntity> users = userService.searchUsers(keyword, me.tenantId(), Math.min(limit, 50));
        List<Map<String, Object>> result = users.stream()
                .filter(u -> u.getId() != null && !u.getId().equals(me.userId()))
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("realName", u.getRealName());
                    m.put("username", u.getUsername());
                    m.put("avatarUrl", u.getAvatarUrl());
                    return m;
                })
                .toList();
        return ApiResponse.ok(result);
    }
}
