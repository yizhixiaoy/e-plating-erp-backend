package com.plating.erp.todo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import com.plating.erp.common.api.response.PageResult;
import com.plating.erp.todo.entity.TodoEntity;
import com.plating.erp.todo.entity.TodoHandleLogEntity;
import com.plating.erp.todo.mapper.TodoHandleLogMapper;
import com.plating.erp.todo.mapper.TodoMapper;
import com.plating.erp.todo.service.TodoService;
import com.plating.erp.todo.vo.TodoVo;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.message.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class TodoServiceImpl implements TodoService {
    private static final Logger log = LoggerFactory.getLogger(TodoServiceImpl.class);

    private static final Set<String> VALID_ACTIONS = Set.of(
            "AGREE", "REJECT", "TRANSFER", "COMPLETE", "IGNORE"
    );

    private final TodoMapper todoMapper;
    private final TodoHandleLogMapper todoHandleLogMapper;
    private final UserMapper userMapper;
    private final EmailService emailService;

    public TodoServiceImpl(TodoMapper todoMapper, 
                          TodoHandleLogMapper todoHandleLogMapper,
                          UserMapper userMapper,
                          EmailService emailService) {
        this.todoMapper = todoMapper;
        this.todoHandleLogMapper = todoHandleLogMapper;
        this.userMapper = userMapper;
        this.emailService = emailService;
    }

    @Override
    public PageResult<TodoEntity> myList(int pageNum, int pageSize, Long assigneeId, TodoVo.QueryReq query) {
        LambdaQueryWrapper<TodoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TodoEntity::getAssigneeId, assigneeId);
        if (query != null) {
            if (query.todoType() != null && !query.todoType().isBlank()) {
                wrapper.eq(TodoEntity::getTodoType, query.todoType());
            }
            if (query.status() != null) {
                wrapper.eq(TodoEntity::getStatus, query.status());
            }
            if (query.priority() != null) {
                wrapper.eq(TodoEntity::getPriority, query.priority());
            }
            if (query.keyword() != null && !query.keyword().isBlank()) {
                wrapper.and(w -> w.like(TodoEntity::getTitle, query.keyword())
                        .or().like(TodoEntity::getContent, query.keyword()));
            }
        }
        // 排序：未读优先、优先级高优先、再按创建时间倒序
        wrapper.orderByAsc(TodoEntity::getReadStatus)
                .orderByDesc(TodoEntity::getPriority)
                .orderByDesc(TodoEntity::getCreatedAt);

        Page<TodoEntity> page = todoMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Override
    public TodoEntity getById(Long id, Long currentUserId) {
        TodoEntity todo = todoMapper.selectById(id);
        if (todo == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "待办不存在");
        }
        boolean owner = todo.getAssigneeId() != null && todo.getAssigneeId().equals(currentUserId);
        boolean creator = todo.getCreatorId() != null && todo.getCreatorId().equals(currentUserId);
        if (!owner && !creator) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该待办");
        }
        return todo;
    }

    @Override
    @Transactional
    public TodoEntity create(TodoVo.CreateReq req, Long creatorId, Long tenantId) {
        TodoEntity entity = new TodoEntity();
        entity.setTenantId(tenantId == null ? 0L : tenantId);
        entity.setAssigneeId(req.assigneeId());
        entity.setCreatorId(creatorId);
        entity.setTodoType(req.todoType() == null ? "TASK" : req.todoType());
        entity.setPriority(req.priority() == null ? 1 : req.priority());
        entity.setTitle(req.title());
        entity.setContent(req.content());
        entity.setBizModule(req.bizModule());
        entity.setBizRefId(req.bizRefId());
        entity.setBizRefUrl(req.bizRefUrl());
        entity.setStatus(0);
        entity.setReadStatus(0);
        entity.setDeadline(req.deadline());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(0);
        entity.setPushEmail(req.pushEmail() != null ? req.pushEmail() : 0);
        todoMapper.insert(entity);
        
        // 如果启用了邮件推送,给处理人发送邮件
        if (entity.getPushEmail() != null && entity.getPushEmail() == 1) {
            try {
                UserEntity assignee = userMapper.selectById(req.assigneeId());
                if (assignee != null && assignee.getEmail() != null && !assignee.getEmail().isBlank()) {
                    emailService.sendTodoNotificationEmail(
                        assignee.getId(),
                        assignee.getEmail(),
                        assignee.getRealName(),
                        entity.getTitle(),
                        entity.getTodoType(),
                        entity.getPriority(),
                        entity.getContent()
                    );
                }
            } catch (Exception e) {
                log.error("发送待办通知邮件异常: assigneeId={}", req.assigneeId(), e);
            }
        }
        
        return entity;
    }

    @Override
    @Transactional
    public TodoEntity handle(Long id, TodoVo.HandleReq req, Long currentUserId) {
        TodoEntity todo = todoMapper.selectById(id);
        if (todo == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "待办不存在");
        }
        if (todo.getAssigneeId() == null || !todo.getAssigneeId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅处理人可操作该待办");
        }
        if (todo.getStatus() != null && todo.getStatus() != 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "待办已处理，不可重复操作");
        }
        String action = req.action() == null ? "" : req.action().toUpperCase();
        if (!VALID_ACTIONS.contains(action)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "非法的处理动作");
        }

        // 状态映射
        int newStatus = switch (action) {
            case "AGREE", "REJECT", "COMPLETE" -> 1; // 已处理
            case "IGNORE" -> 2;
            case "TRANSFER" -> 3;
            default -> 1;
        };

        if ("TRANSFER".equals(action) && req.targetUserId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "转交时必须指定目标用户");
        }

        todo.setStatus(newStatus);
        todo.setHandleAction(action);
        todo.setHandleRemark(req.remark());
        todo.setHandledAt(LocalDateTime.now());
        if (todo.getReadStatus() != null && todo.getReadStatus() == 0) {
            todo.setReadStatus(1);
            todo.setReadTime(LocalDateTime.now());
        }
        todo.setUpdatedAt(LocalDateTime.now());
        todoMapper.updateById(todo);

        // 转交：派生新待办给目标用户，并保留原条目（标记已转交）
        if ("TRANSFER".equals(action)) {
            TodoEntity transferred = new TodoEntity();
            transferred.setTenantId(todo.getTenantId());
            transferred.setAssigneeId(req.targetUserId());
            transferred.setCreatorId(currentUserId);
            transferred.setTodoType(todo.getTodoType());
            transferred.setPriority(todo.getPriority());
            transferred.setTitle(todo.getTitle());
            transferred.setContent(todo.getContent());
            transferred.setBizModule(todo.getBizModule());
            transferred.setBizRefId(todo.getBizRefId());
            transferred.setBizRefUrl(todo.getBizRefUrl());
            transferred.setStatus(0);
            transferred.setReadStatus(0);
            transferred.setDeadline(todo.getDeadline());
            transferred.setCreatedAt(LocalDateTime.now());
            transferred.setUpdatedAt(LocalDateTime.now());
            transferred.setDeleted(0);
            todoMapper.insert(transferred);
        }

        // 处理日志
        TodoHandleLogEntity log = new TodoHandleLogEntity();
        log.setTenantId(todo.getTenantId());
        log.setTodoId(todo.getId());
        log.setOperatorId(currentUserId);
        log.setAction(action);
        log.setRemark(req.remark());
        log.setTargetUser(req.targetUserId());
        log.setCreatedAt(LocalDateTime.now());
        todoHandleLogMapper.insert(log);

        return todo;
    }

    @Override
    @Transactional
    public void markRead(Long id, Long currentUserId) {
        TodoEntity todo = todoMapper.selectById(id);
        if (todo == null) {
            return;
        }
        if (todo.getAssigneeId() == null || !todo.getAssigneeId().equals(currentUserId)) {
            return; // 静默忽略，不抛异常
        }
        if (todo.getReadStatus() != null && todo.getReadStatus() == 1) {
            return;
        }
        todo.setReadStatus(1);
        todo.setReadTime(LocalDateTime.now());
        todoMapper.updateById(todo);
    }

    @Override
    @Transactional
    public int batchReadAll(Long currentUserId) {
        List<TodoEntity> unread = todoMapper.selectList(new LambdaQueryWrapper<TodoEntity>()
                .eq(TodoEntity::getAssigneeId, currentUserId)
                .eq(TodoEntity::getReadStatus, 0));
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (TodoEntity t : unread) {
            t.setReadStatus(1);
            t.setReadTime(now);
            todoMapper.updateById(t);
            count++;
        }
        return count;
    }

    @Override
    @Transactional
    public boolean delete(Long id, Long currentUserId) {
        TodoEntity todo = todoMapper.selectById(id);
        if (todo == null) {
            return false;
        }
        if (todo.getAssigneeId() == null || !todo.getAssigneeId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅处理人可删除该待办");
        }
        return todoMapper.deleteById(id) > 0;
    }

    @Override
    public TodoVo.Stats stats(Long currentUserId) {
        long pending = countByStatus(currentUserId, 0);
        long done = countByStatus(currentUserId, 1);
        long ignored = countByStatus(currentUserId, 2);
        long transferred = countByStatus(currentUserId, 3);

        Long overdue = todoMapper.selectCount(new LambdaQueryWrapper<TodoEntity>()
                .eq(TodoEntity::getAssigneeId, currentUserId)
                .eq(TodoEntity::getStatus, 0)
                .isNotNull(TodoEntity::getDeadline)
                .lt(TodoEntity::getDeadline, LocalDateTime.now()));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Long todayNew = todoMapper.selectCount(new LambdaQueryWrapper<TodoEntity>()
                .eq(TodoEntity::getAssigneeId, currentUserId)
                .ge(TodoEntity::getCreatedAt, startOfDay));

        return new TodoVo.Stats(pending, done, ignored, transferred,
                overdue == null ? 0 : overdue, todayNew == null ? 0 : todayNew);
    }

    @Override
    public long countPending(Long currentUserId) {
        return countByStatus(currentUserId, 0);
    }

    private long countByStatus(Long userId, Integer status) {
        Long c = todoMapper.selectCount(new LambdaQueryWrapper<TodoEntity>()
                .eq(TodoEntity::getAssigneeId, userId)
                .eq(TodoEntity::getStatus, status));
        return c == null ? 0 : c;
    }
}
