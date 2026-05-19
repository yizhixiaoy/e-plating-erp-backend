package com.plating.erp.todo.controller;

import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.common.api.ApiResponse;
import com.plating.erp.common.api.response.CommonResponses;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.todo.entity.TodoEntity;
import com.plating.erp.todo.service.TodoService;
import com.plating.erp.todo.vo.TodoVo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * 我的待办列表
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<TodoEntity>> myList(@RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "20") Integer pageSize,
                                                      @RequestParam(required = false) String todoType,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) Integer priority,
                                                      @RequestParam(required = false) String keyword) {
        var u = SecurityUtils.currentUser();
        TodoVo.QueryReq query = new TodoVo.QueryReq(todoType, status, priority, keyword);
        return ApiResponse.ok(todoService.myList(pageNum, pageSize, u.userId(), query));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TodoEntity> detail(@PathVariable Long id) {
        var u = SecurityUtils.currentUser();
        return ApiResponse.ok(todoService.getById(id, u.userId()));
    }

    /**
     * 创建待办（业务模块/管理员）
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "待办中心", operateType = "CREATE", bizModule = "todo", fieldName = "title")
    public ApiResponse<TodoEntity> create(@Valid @RequestBody TodoVo.CreateReq req) {
        var u = SecurityUtils.currentUser();
        return ApiResponse.ok(todoService.create(req, u.userId(), u.tenantId()));
    }

    /**
     * 处理待办（同意/拒绝/完成/转交/忽略）
     */
    @PostMapping("/{id}/handle")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "待办中心", operateType = "STATUS", bizModule = "todo", fieldName = "handleAction")
    public ApiResponse<TodoEntity> handle(@PathVariable Long id, @Valid @RequestBody TodoVo.HandleReq req) {
        var u = SecurityUtils.currentUser();
        return ApiResponse.ok(todoService.handle(id, req, u.userId()));
    }

    /**
     * 标记已读（单条）
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CommonResponses.SuccessResponse> markRead(@PathVariable Long id) {
        var u = SecurityUtils.currentUser();
        todoService.markRead(id, u.userId());
        return ApiResponse.ok(new CommonResponses.SuccessResponse(true));
    }

    /**
     * 一键全部已读
     */
    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Integer> readAll() {
        var u = SecurityUtils.currentUser();
        return ApiResponse.ok(todoService.batchReadAll(u.userId()));
    }

    /**
     * 删除（软删，仅 assignee 可删）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(module = "待办中心", operateType = "DELETE", bizModule = "todo", fieldName = "title")
    public ApiResponse<CommonResponses.DeleteResponse> delete(@PathVariable Long id) {
        var u = SecurityUtils.currentUser();
        boolean ok = todoService.delete(id, u.userId());
        return ApiResponse.ok(new CommonResponses.DeleteResponse(ok, id));
    }

    /**
     * 统计
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TodoVo.Stats> stats() {
        var u = SecurityUtils.currentUser();
        return ApiResponse.ok(todoService.stats(u.userId()));
    }
}
