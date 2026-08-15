package com.plating.erp.message.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plating.erp.message.entity.NoticeUserEntity;
import com.plating.erp.message.mapper.NoticeUserMapper;
import com.plating.erp.message.service.NoticeUserService;
import org.springframework.stereotype.Service;

@Service
public class NoticeUserServiceImpl extends ServiceImpl<NoticeUserMapper, NoticeUserEntity> implements NoticeUserService {
}
