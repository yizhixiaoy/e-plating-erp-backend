package com.plating.erp.audit;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.audit.entity.BizLogEntity;
import com.plating.erp.audit.entity.OperLogEntity;
import com.plating.erp.audit.mapper.BizLogMapper;
import com.plating.erp.audit.mapper.OperLogMapper;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.common.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.*;

@Aspect
@Component
public class AuditLogAspect {
    private final OperLogMapper operLogMapper;
    private final BizLogMapper bizLogMapper;
    private final ObjectMapper objectMapper;

    public AuditLogAspect(OperLogMapper operLogMapper, BizLogMapper bizLogMapper, ObjectMapper objectMapper) {
        this.operLogMapper = operLogMapper;
        this.bizLogMapper = bizLogMapper;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        CurrentUser user = SecurityUtils.currentUser();
        int status = 0;
        try {
            return pjp.proceed();
        } catch (Throwable ex) {
            status = 1;
            throw ex;
        } finally {
            OperLogEntity oper = new OperLogEntity();
            oper.setId(IdWorker.getId());
            oper.setTenantId(user.tenantId());
            oper.setModuleTitle(auditLog.module());
            oper.setOperateType(auditLog.operateType());
            oper.setUserId(user.userId());
            oper.setRequestParams(maskAndSerializeArgs(pjp.getArgs(), auditLog.maskFields()));
            oper.setStatus(status);
            operLogMapper.insert(oper);

            if (!auditLog.bizModule().isBlank() && !auditLog.fieldName().isBlank()) {
                BizLogEntity biz = new BizLogEntity();
                biz.setId(IdWorker.getId());
                biz.setTenantId(user.tenantId());
                biz.setBizModule(auditLog.bizModule());
                biz.setBizId(oper.getId());
                biz.setFieldName(auditLog.fieldName());
                bizLogMapper.insert(biz);
            }
        }
    }

    private String maskAndSerializeArgs(Object[] args, String[] maskFields) {
        Set<String> masks = new HashSet<>();
        for (String f : maskFields) {
            masks.add(f.toLowerCase(Locale.ROOT));
        }
        List<Object> result = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                result.add(null);
                continue;
            }
            if (arg.getClass().getName().startsWith("jakarta.servlet") || arg.getClass().getName().startsWith("org.springframework")) {
                continue;
            }
            Object normalized = objectMapper.convertValue(arg, Object.class);
            result.add(maskObject(normalized, masks));
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private Object maskObject(Object value, Set<String> masks) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (masks.contains(key.toLowerCase(Locale.ROOT))) {
                    out.put(key, "***");
                } else {
                    out.put(key, maskObject(entry.getValue(), masks));
                }
            }
            return out;
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>();
            for (Object item : list) {
                out.add(maskObject(item, masks));
            }
            return out;
        }
        return value;
    }
}
