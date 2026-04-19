package com.plating.erp.message.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plating.erp.message.entity.NoticeEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 公告数据访问层
 * 
 * 注意：
 * - 定时任务中使用的方法需要跳过租户拦截器
 * - 必须在 Service 层手动遍历所有租户处理
 */
@Mapper
public interface NoticeMapper extends BaseMapper<NoticeEntity> {
    
    /**
     * 查询所有租户的待发布公告（跳过租户拦截器）
     * 仅用于定时任务场景
     * 
     * @return 待发布公告列表
     */
    @InterceptorIgnore(tenantLine = "true")
    List<NoticeEntity> selectDueNoticesIgnoreTenant();
}
