package com.plating.erp.todo.service;

import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.todo.entity.TodoEntity;
import com.plating.erp.todo.vo.TodoVo;

public interface TodoService {

    /** 我的待办列表（按当前登录人 = assignee） */
    PageResult<TodoEntity> myList(int pageNum, int pageSize, Long assigneeId, TodoVo.QueryReq query);

    /** 详情（带权限检查：仅当前用户为 assignee 或 creator 可看） */
    TodoEntity getById(Long id, Long currentUserId);

    /** 创建待办（业务模块/管理员调用） */
    TodoEntity create(TodoVo.CreateReq req, Long creatorId, Long tenantId);

    /** 处理待办（AGREE/REJECT/COMPLETE/TRANSFER/IGNORE） */
    TodoEntity handle(Long id, TodoVo.HandleReq req, Long currentUserId);

    /** 标记已读（单条） */
    void markRead(Long id, Long currentUserId);

    /** 批量标记全部为已读 */
    int batchReadAll(Long currentUserId);

    /** 删除（仅 assignee 可删，软删） */
    boolean delete(Long id, Long currentUserId);

    /** 统计 */
    TodoVo.Stats stats(Long currentUserId);

    /** 待处理数量（工作台快速接入） */
    long countPending(Long currentUserId);
}
