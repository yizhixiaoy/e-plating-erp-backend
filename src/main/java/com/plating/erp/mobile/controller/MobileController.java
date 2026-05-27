package com.plating.erp.mobile.controller;

import com.plating.erp.auth.service.AuthService;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.common.util.FileUploadUtils;
import com.plating.erp.iam.entity.DeptEntity;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.DeptMapper;
import com.plating.erp.iam.service.DeptService;
import com.plating.erp.iam.service.UserService;
import com.plating.erp.message.entity.EmailRecordEntity;
import com.plating.erp.message.entity.NoticeUserEntity;
import com.plating.erp.message.entity.SmsRecordEntity;
import com.plating.erp.message.mapper.EmailRecordMapper;
import com.plating.erp.message.mapper.NoticeUserMapper;
import com.plating.erp.message.mapper.SmsRecordMapper;
import com.plating.erp.message.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plating.erp.platform.entity.TenantEntity;
import com.plating.erp.platform.service.TenantService;
import com.plating.erp.todo.entity.TodoEntity;
import com.plating.erp.todo.service.TodoService;
import com.plating.erp.todo.vo.TodoVo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 移动端聚合接口入口（/api/v1/mobile/*）
 */
@RestController
@RequestMapping("/api/v1/mobile")
public class MobileController {
    private static final String SETTINGS_KEY_PREFIX = "erp:mobile:settings:";

    private final MessageService messageService;
    private final AuthService authService;
    private final NoticeUserMapper noticeUserMapper;
    private final TodoService todoService;
    private final UserService userService;
    private final DeptService deptService;
    private final DeptMapper deptMapper;
    private final TenantService tenantService;
    private final StringRedisTemplate redisTemplate;
    private final EmailRecordMapper emailRecordMapper;
    private final SmsRecordMapper smsRecordMapper;

    public MobileController(MessageService messageService,
                            AuthService authService,
                            NoticeUserMapper noticeUserMapper,
                            TodoService todoService,
                            UserService userService,
                            DeptService deptService,
                            DeptMapper deptMapper,
                            TenantService tenantService,
                            StringRedisTemplate redisTemplate,
                            EmailRecordMapper emailRecordMapper,
                            SmsRecordMapper smsRecordMapper) {
        this.messageService = messageService;
        this.authService = authService;
        this.noticeUserMapper = noticeUserMapper;
        this.todoService = todoService;
        this.userService = userService;
        this.deptService = deptService;
        this.deptMapper = deptMapper;
        this.tenantService = tenantService;
        this.redisTemplate = redisTemplate;
        this.emailRecordMapper = emailRecordMapper;
        this.smsRecordMapper = smsRecordMapper;
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
     */
    @PostMapping("/messages/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAllRead(CurrentUser user) {
        if (user == null || user.userId() == null) {
            return ApiResponse.ok(null);
        }
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

    @GetMapping("/todos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TodoEntity> getTodoDetail(@PathVariable Long id, CurrentUser user) {
        return ApiResponse.ok(todoService.getById(id, user.userId()));
    }

    @PostMapping("/todos/{id}/handle")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TodoEntity> handleTodo(@PathVariable Long id,
                                              @RequestBody TodoVo.HandleReq body,
                                              CurrentUser user) {
        return ApiResponse.ok(todoService.handle(id, body, user.userId()));
    }

    @PutMapping("/todos/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markTodoRead(@PathVariable Long id, CurrentUser user) {
        todoService.markRead(id, user.userId());
        return ApiResponse.ok(null);
    }

    @GetMapping("/todos/stats")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TodoVo.Stats> todoStats(CurrentUser user) {
        return ApiResponse.ok(todoService.stats(user.userId()));
    }

    // ==================== 工作台 ====================

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

    @GetMapping("/workbench/quick-access")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> quickAccess() {
        List<Map<String, Object>> list = List.of(
                Map.of("key", "scan", "label", "扫一扫"),
                Map.of("key", "chat", "label", "聊天"),
                Map.of("key", "contacts", "label", "通讯录"),
                Map.of("key", "message", "label", "消息"),
                Map.of("key", "todo", "label", "待办")
        );
        return ApiResponse.ok(list);
    }

    // ==================== 通讯录 ====================

    @GetMapping("/contacts/depts")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> contactsDepts(CurrentUser user) {
        List<DeptEntity> depts = deptService.listAll(user.tenantId());
        // 构建部门树（两层简化：parentId=0 为顶层）
        Map<Long, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();
        for (DeptEntity d : depts) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", d.getId());
            node.put("deptName", d.getDeptName());
            node.put("parentId", d.getParentId());
            node.put("children", new ArrayList<Map<String, Object>>());
            nodeMap.put(d.getId(), node);
        }
        for (Map<String, Object> node : nodeMap.values()) {
            Long parentId = (Long) node.get("parentId");
            if (parentId == null || parentId == 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) nodeMap.get(parentId).get("children");
                children.add(node);
            }
        }
        return ApiResponse.ok(roots);
    }

    @GetMapping("/contacts/users")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> contactsUsers(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "20") Integer pageSize,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String deptId,
                                                          CurrentUser user) {
        Long filterDeptId = null;
        if (deptId != null && !deptId.isBlank()) {
            try { filterDeptId = Long.parseLong(deptId); } catch (NumberFormatException ignored) {}
        }
        // status=0 表示在职
        PageResult<UserEntity> page = userService.page(pageNum, pageSize, filterDeptId, 0, keyword, user.tenantId());
        // 解析部门名称
        Set<Long> deptIds = page.records().stream().map(UserEntity::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> deptNameMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            for (Long did : deptIds) {
                DeptEntity dept = deptMapper.selectById(did);
                if (dept != null) deptNameMap.put(did, dept.getDeptName());
            }
        }
        List<Map<String, Object>> records = new ArrayList<>();
        for (UserEntity u : page.records()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("realName", u.getRealName());
            m.put("username", u.getUsername());
            m.put("phone", u.getPhone());
            m.put("email", u.getEmail());
            m.put("deptName", u.getDeptId() != null ? deptNameMap.getOrDefault(u.getDeptId(), "") : "");
            m.put("position", u.getPosition());
            m.put("avatarUrl", u.getAvatarUrl() != null && !u.getAvatarUrl().isEmpty()
                    ? FileUploadUtils.getResourceUrl(u.getAvatarUrl(), "avatar.jpg") : null);
            records.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", page.total());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return ApiResponse.ok(result);
    }

    @GetMapping("/contacts/users/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> contactsUserDetail(@PathVariable Long id, CurrentUser user) {
        UserEntity u = userService.getById(id);
        if (u == null || !u.getTenantId().equals(user.tenantId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        String deptName = null;
        if (u.getDeptId() != null) {
            DeptEntity dept = deptMapper.selectById(u.getDeptId());
            if (dept != null) deptName = dept.getDeptName();
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", u.getId());
        resp.put("realName", u.getRealName());
        resp.put("username", u.getUsername());
        resp.put("phone", u.getPhone());
        resp.put("email", u.getEmail());
        resp.put("deptName", deptName);
        resp.put("position", u.getPosition());
        resp.put("avatarUrl", u.getAvatarUrl() != null && !u.getAvatarUrl().isEmpty()
                ? FileUploadUtils.getResourceUrl(u.getAvatarUrl(), "avatar.jpg") : null);
        resp.put("status", u.getStatus());
        resp.put("createTime", u.getCreatedAt());
        return ApiResponse.ok(resp);
    }

    // ==================== 个人中心 ====================

    @GetMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> meProfile(CurrentUser user) {
        UserEntity u = userService.getById(user.userId());
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        String deptName = null;
        if (u.getDeptId() != null) {
            DeptEntity dept = deptMapper.selectById(u.getDeptId());
            if (dept != null) deptName = dept.getDeptName();
        }
        String tenantName = null;
        if (u.getTenantId() != null) {
            TenantEntity tenant = tenantService.getById(u.getTenantId());
            if (tenant != null) tenantName = tenant.getTenantName();
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("userId", String.valueOf(u.getId()));
        resp.put("tenantId", String.valueOf(u.getTenantId()));
        resp.put("username", u.getUsername());
        resp.put("realName", u.getRealName());
        resp.put("phone", u.getPhone());
        resp.put("email", u.getEmail());
        resp.put("deptName", deptName);
        resp.put("position", u.getPosition());
        resp.put("tenantName", tenantName);
        resp.put("avatarUrl", u.getAvatarUrl() != null && !u.getAvatarUrl().isEmpty()
                ? FileUploadUtils.getResourceUrl(u.getAvatarUrl(), "avatar.jpg") : null);
        return ApiResponse.ok(resp);
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CommonResponses.SuccessResponse> mePassword(@RequestBody Map<String, String> body,
                                                                   CurrentUser user) {
        String oldPwd = body == null ? null : body.get("oldPassword");
        String newPwd = body == null ? null : body.get("newPassword");
        if (oldPwd == null || oldPwd.isEmpty()) {
            return ApiResponse.error(400, "旧密码不能为空");
        }
        if (newPwd == null || newPwd.length() < 8 || newPwd.length() > 20) {
            return ApiResponse.error(400, "新密码长度需在8-20位");
        }
        UserEntity u = userService.getById(user.userId());
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!userService.checkPassword(oldPwd, u.getPasswordHash())) {
            return ApiResponse.error(400, "旧密码不正确");
        }
        userService.updatePassword(user.userId(), newPwd);
        return ApiResponse.ok(new CommonResponses.SuccessResponse(true));
    }

    @GetMapping("/me/settings")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> getSettings(CurrentUser user) {
        Map<String, Object> defaults = defaultSettings();
        if (user != null && user.userId() != null) {
            String json = redisTemplate.opsForValue().get(SETTINGS_KEY_PREFIX + user.userId());
            if (json != null && !json.isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stored = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(json, Map.class);
                    defaults.putAll(stored);
                } catch (Exception ignored) {}
            }
        }
        return ApiResponse.ok(defaults);
    }

    @PutMapping("/me/settings")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> putSettings(@RequestBody Map<String, Object> body,
                                                        CurrentUser user) {
        if (user != null && user.userId() != null && body != null) {
            try {
                String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
                redisTemplate.opsForValue().set(SETTINGS_KEY_PREFIX + user.userId(), json, Duration.ofDays(90));
            } catch (Exception ignored) {}
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

    // ==================== 公司信息 ====================

    @GetMapping("/me/company")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> companyInfo(CurrentUser user) {
        TenantEntity tenant = tenantService.getById(user.tenantId());
        if (tenant == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租户不存在");
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("tenantName", tenant.getTenantName());
        resp.put("shortCode", tenant.getShortCode());
        resp.put("contactName", tenant.getContactName());
        resp.put("phone", tenant.getPhone());
        resp.put("logoUrl", tenant.getLogoUrl());
        return ApiResponse.ok(resp);
    }

    // ==================== 发送记录 ====================

    @GetMapping("/me/email-records")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> myEmailRecords(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer sendStatus,
            CurrentUser user) {
        LambdaQueryWrapper<EmailRecordEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(EmailRecordEntity::getOperatorId, user.userId());
        if (sendStatus != null) {
            qw.eq(EmailRecordEntity::getSendStatus, sendStatus);
        }
        qw.orderByDesc(EmailRecordEntity::getCreatedAt);
        var page = emailRecordMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> records = new ArrayList<>();
        for (EmailRecordEntity e : page.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("receiverEmail", e.getReceiverEmail());
            m.put("subject", e.getSubject());
            m.put("sendStatus", e.getSendStatus());
            m.put("failReason", e.getFailReason());
            m.put("sentTime", e.getSentTime());
            m.put("createdAt", e.getCreatedAt());
            records.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return ApiResponse.ok(result);
    }

    @GetMapping("/me/sms-records")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> mySmsRecords(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer sendStatus,
            @RequestParam(required = false) String smsType,
            CurrentUser user) {
        LambdaQueryWrapper<SmsRecordEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(SmsRecordEntity::getOperatorId, user.userId());
        if (sendStatus != null) {
            qw.eq(SmsRecordEntity::getSendStatus, sendStatus);
        }
        if (smsType != null && !smsType.isBlank()) {
            qw.eq(SmsRecordEntity::getSmsType, smsType);
        }
        qw.orderByDesc(SmsRecordEntity::getCreatedAt);
        var page = smsRecordMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), qw);
        List<Map<String, Object>> records = new ArrayList<>();
        for (SmsRecordEntity s : page.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("receiverPhone", s.getReceiverPhone());
            m.put("smsType", s.getSmsType());
            m.put("content", s.getContent());
            m.put("sendStatus", s.getSendStatus());
            m.put("failReason", s.getFailReason());
            m.put("sentTime", s.getSentTime());
            m.put("createdAt", s.getCreatedAt());
            records.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return ApiResponse.ok(result);
    }

    // ==================== 认证 ====================

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
