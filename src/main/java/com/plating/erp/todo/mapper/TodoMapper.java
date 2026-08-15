package com.plating.erp.todo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.todo.entity.TodoEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoMapper extends BaseMapper<TodoEntity> {
}
