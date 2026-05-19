package com.plating.erp.mobile.controller;

import com.plating.erp.auth.service.AuthService;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.message.entity.NoticeUserEntity;
import com.plating.erp.message.mapper.NoticeUserMapper;
import com.plating.erp.message.service.MessageService;
import com.plating.erp.todo.entity.TodoEntity;
import com.plating.erp.todo.service.TodoService;
import com.plating.erp.todo.vo.TodoVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动端聚合接口入口（/api/v1/mobile/*）
 *
 * <p>真实业务模块未就绪前，部分接口返回 mock 数据，标识为 // TODO(real):</p>
 */
@RestController
@RequestMapping("/api/v1/mobile")
public class MobileController {
    private final MessageService messageService;
    private final AuthService authService;
    private final NoticeUserMapper noticeUserMapper;
    private final TodoService todoService;

    public MobileController(MessageService messageService,
                            AuthService authService,
                            NoticeUserMapper noticeUserMapper,
                            TodoService todoService) {
        this.messageService = messageService;
        this.authService = authService;
        this.noticeUserMapper = noticeUserMapper;
        this.todoService = todoService;
    }

    // ==================== 消息 ====================

    @GetMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<?>> getMessages(CurrentUser user) {
        var result = messageService.myNotices(1, 100, user.userId(), null);
        return ApiResponse.ok(result.records());
    }

    @PutMapping("/messages/{noticeId}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAsRead(@PathVariable Long noticeId, CurrentUser user) {
        messageService.readNotice(noticeId, user.userId());
        return ApiResponse.ok(null);
    }

    /**
     * 一键已读
     * TODO(real): 移至 MessageService.batchReadAll(userId)
     */
    @PostMapping("/messages/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAllRead(CurrentUser user) {
        if (user == null || user.userId() == null) {
            return ApiResponse.ok(null);
        }
        // 直接定位未读 NoticeUser 记录，最多 1000 条避免一次过多
        List<NoticeUserEntity> unread = noticeUserMapper.selectVisiblePage(user.userId(), 0, 0L, 1000);
        if (unread != null) {
            for (NoticeUserEntity nu : unread) {
                if (nu == null || nu.getNoticeId() == null) continue;
                try {
                    messageService.readNotice(nu.getNoticeId(), user.userId());
                } catch (Exception ignore) { /* 不阻断整体流程 */ }
            }
        }
        return ApiResponse.ok(null);
    }

    // ==================== 待办 ====================

    /**
     * 待办列表（接入真实 TodoService）
     */
    @GetMapping("/todos")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<TodoEntity>> getTodos(@RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "20") Integer pageSize,
                                                        @RequestParam(required = false) Integer status,
                                                        @RequestParam(required = false) String todoType,
                                                        CurrentUser user) {
        if (user == null || user.userId() == null) {
            return ApiResponse.ok(new PageResult<>(List.of(), 0L));
        }
        TodoVo.QueryReq q = new TodoVo.QueryReq(todoType, status, null, null);
        return ApiResponse.ok(todoService.myList(pageNum, pageSize, user.userId(), q));
    }

    /**
     * 待办详情
     */
    @GetMapping("/todos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TodoEntity> getTodoDetail(@PathVariable Long id, CurrentUser user) {
        return ApiResponse.ok(todoService.getById(id, user.userId()));
    }

    /**
     * 待办处理（同意/拒绝/转交/完成/忽略）
     */
    @PostMapping("/todos/{id}/handle")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TodoEntity> handleTodo(@PathVariable Long id,
                                              @RequestBody TodoVo.HandleReq body,
                                              CurrentUser user) {
        return ApiResponse.ok(todoService.handle(id, body, user.userId()));
    }

    /**
     * 待办单条已读
     */
    @PutMapping("/todos/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markTodoRead(@PathVariable Long id, CurrentUser user) {
        todoService.markRead(id, user.userId());
        return ApiResponse.ok(null);
    }

    /**
     * 待办统计
     */
    @GetMapping("/todos/stats")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TodoVo.Stats> todoStats(CurrentUser user) {
        return ApiResponse.ok(todoService.stats(user.userId()));
    }

    // ==================== 工作台 ====================

    /**
     * 工作台聚合统计
     * TODO(real): 接入真实统计 service（消息/待办/审计/在线人数）
     */
    @GetMapping("/workbench/stats")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> workbenchStats(CurrentUser user) {
        long unread = 0;
        long pendingTodos = 0;
        if (user != null && user.userId() != null) {
            Long c = noticeUserMapper.countVisibleForUser(user.userId(), 0);
            unread = c == null ? 0 : c;
            pendingTodos = todoService.countPending(user.userId());
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("unreadMessages", unread);
        resp.put("pendingTodos", pendingTodos);
        resp.put("todayLogs", 0);
        resp.put("onlineUsers", 0);
        return ApiResponse.ok(resp);
    }

    /**
     * 快捷入口配置
     * TODO(real): 后续从用户偏好/角色配置读取
     */
    @GetMapping("/workbench/quick-access")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> quickAccess() {
        List<Map<String, Object>> list = List.of(
                Map.of("key", "scan", "label", "扫一扫"),
                Map.of("key", "contacts", "label", "通讯录"),
                Map.of("key", "message", "label", "消息"),
                Map.of("key", "todo", "label", "待办"),
                Map.of("key", "settings", "label", "设置")
        );
        return ApiResponse.ok(list);
    }

    // ==================== 通讯录 ====================

    /**
     * 部门树
     * TODO(real): 调用 DeptService.tree(tenantId) 替换 mock
     */
    @GetMapping("/contacts/depts")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> contactsDepts(CurrentUser user) {
        List<Map<String, Object>> list = List.of(
                deptNode("1", "总部", 0),
                deptNode("2", "生产部", 0),
                deptNode("3", "质量部", 0),
                deptNode("4", "销售部", 0)
        );
        return ApiResponse.ok(list);
    }

    private Map<String, Object> deptNode(String id, String name, int count) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("deptName", name);
        m.put("userCount", count);
        return m;
    }

    /**
     * 员工列表
     * TODO(real): 改造 UserService 提供 mobile 友好的列表（无后台权限要求）
     */
    @GetMapping("/contacts/users")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> contactsUsers(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "20") Integer pageSize,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String deptId,
                                                          CurrentUser user) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("records", List.of());
        page.put("total", 0);
        page.put("pageNum", pageNum);
        page.put("pageSize", pageSize);
        return ApiResponse.ok(page);
    }

    /**
     * 员工详情
     * TODO(real): 调用 UserService.getProfileById(id) 替换 mock
     */
    @GetMapping("/contacts/users/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> contactsUserDetail(@PathVariable String id) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", id);
        resp.put("realName", "");
        resp.put("username", "");
        resp.put("deptName", "");
        resp.put("positionName", "");
        resp.put("phone", "");
        resp.put("email", "");
        resp.put("status", 0);
        return ApiResponse.ok(resp);
    }

    // ==================== 个人中心 ====================

    /**
     * 个人资料
     * TODO(real): 接入 UserService.getCurrentProfile()
     */
    @GetMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> meProfile(CurrentUser user) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("userId", user.userId() == null ? "" : String.valueOf(user.userId()));
        resp.put("tenantId", user.tenantId() == null ? "" : String.valueOf(user.tenantId()));
        resp.put("username", user.username());
        resp.put("realName", "");
        resp.put("deptName", "");
        resp.put("positionName", "");
        resp.put("tenantName", "");
        resp.put("avatar", "");
        return ApiResponse.ok(resp);
    }

    /**
     * 修改密码
     * TODO(real): 接入 UserService.changePassword(userId, oldPwd, newPwd)，并使旧 token 失效
     */
    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CommonResponses.SuccessResponse> mePassword(@RequestBody Map<String, String> body,
                                                                   CurrentUser user) {
        // mock：仅做最低校验
        String oldPwd = body == null ? null : body.get("oldPassword");
        String newPwd = body == null ? null : body.get("newPassword");
        if (oldPwd == null || newPwd == null || newPwd.length() < 8) {
            return ApiResponse.error(400, "密码不合规");
        }
        return ApiResponse.ok(new CommonResponses.SuccessResponse(true));
    }

    private static final Map<Long, Map<String, Object>> SETTINGS_STORE = new HashMap<>();

    /**
     * 获取个人偏好设置
     * TODO(real): 接入 UserSettingService（持久化到 DB / Redis）
     */
    @GetMapping("/me/settings")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> getSettings(CurrentUser user) {
        Map<String, Object> defaults = defaultSettings();
        Map<String, Object> stored = user == null || user.userId() == null
                ? null : SETTINGS_STORE.get(user.userId());
        if (stored != null) defaults.putAll(stored);
        return ApiResponse.ok(defaults);
    }

    /**
     * 保存个人偏好设置
     * TODO(real): 接入 UserSettingService
     */
    @PutMapping("/me/settings")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> putSettings(@RequestBody Map<String, Object> body,
                                                        CurrentUser user) {
        if (user != null && user.userId() != null && body != null) {
            SETTINGS_STORE.put(user.userId(), new LinkedHashMap<>(body));
        }
        return ApiResponse.ok(body == null ? defaultSettings() : body);
    }

    private Map<String, Object> defaultSettings() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("pushEnabled", true);
        d.put("soundEnabled", true);
        d.put("vibrateEnabled", true);
        d.put("darkMode", false);
        d.put("fontSize", "M");
        return d;
    }

    // ==================== 认证 ====================

    /**
     * 移动端 Token 刷新
     * 复用 AuthService.refreshToken，与 PC 端 /auth/refresh 行为一致
     */
    @PostMapping("/auth/refresh")
    public ApiResponse<?> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body == null ? null : body.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ApiResponse.error(400, "刷新令牌不能为空");
        }
        String newAccessToken = authService.refreshToken(refreshToken);
        return ApiResponse.ok(new CommonResponses.TokenResponse(newAccessToken, 7200));
    }
}
