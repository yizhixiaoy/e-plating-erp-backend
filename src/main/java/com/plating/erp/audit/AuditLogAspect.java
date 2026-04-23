package com.plating.erp.audit;

import com.plating.erp.audit.annotation.AuditLog;
import com.plating.erp.audit.entity.BizLogEntity;
import com.plating.erp.audit.entity.OperLogEntity;
import com.plating.erp.audit.mapper.BizLogMapper;
import com.plating.erp.audit.mapper.OperLogMapper;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.common.util.IpUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

@Aspect
@Component
public class AuditLogAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);
    
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
        long startTime = System.currentTimeMillis();
        CurrentUser user = SecurityUtils.currentUser();
        int status = 0;
        String errorMsg = null;
        Object result = null;
        
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable ex) {
            status = 1;
            errorMsg = ex.getMessage();
            throw ex;
        } finally {
            try {
                long executeTime = System.currentTimeMillis() - startTime;
                Long bizId = extractBizId(result);
                saveOperateLog(pjp, auditLog, user, status, errorMsg, executeTime, result);
                saveBizLog(pjp, auditLog, user, bizId, result);
            } catch (Exception e) {
                log.error("保存审计日志失败", e);
            }
        }
    }
    
    /**
     * 保存操作日志
     */
    private void saveOperateLog(ProceedingJoinPoint pjp, AuditLog auditLog, CurrentUser user, 
                                int status, String errorMsg, long executeTime, Object result) {
        try {
            // 获取HTTP请求信息
            HttpServletRequest request = getCurrentHttpRequest();
            String requestUrl = request != null ? request.getRequestURI() : "";
            String requestMethod = request != null ? request.getMethod() : "";
            String ipAddress = request != null ? IpUtils.getClientIp(request) : "";
            
            // 获取方法签名
            Signature signature = pjp.getSignature();
            String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
            
            OperLogEntity oper = new OperLogEntity();
            oper.setTenantId(user != null ? user.tenantId() : 1L);
            oper.setModuleTitle(auditLog.module());
            oper.setOperateType(auditLog.operateType());
            oper.setUserId(user != null ? user.userId() : 0L);
            oper.setUserName(user != null ? user.username() : "system");
            oper.setRequestUrl(requestUrl);
            oper.setRequestMethod(requestMethod);
            oper.setMethodName(methodName);
            oper.setRequestParams(maskAndSerializeArgs(pjp.getArgs(), auditLog.maskFields()));
            oper.setResponseResult(serializeResult(result));
            oper.setStatus(status);
            oper.setErrorMsg(errorMsg);
            oper.setExecuteTime((int) executeTime);
            oper.setIpAddress(ipAddress);
            oper.setUserAgent(request != null ? request.getHeader("User-Agent") : "");
            
            operLogMapper.insert(oper);
            
            log.debug("操作日志记录成功, userId={}, module={}, type={}, ip={}, time={}ms", 
                    user != null ? user.userId() : 0, auditLog.module(), auditLog.operateType(), 
                    ipAddress, executeTime);
        } catch (Exception e) {
            log.error("保存操作日志失败, module={}", auditLog.module(), e);
        }
    }
    
    /**
     * 保存业务变更日志
     */
    private void saveBizLog(ProceedingJoinPoint pjp, AuditLog auditLog, CurrentUser user, Long bizId, Object result) {
        if (auditLog.bizModule().isBlank() || auditLog.fieldName().isBlank()) {
            return;
        }
        
        try {
            BizLogEntity biz = new BizLogEntity();
            biz.setTenantId(user != null ? user.tenantId() : 1L);
            biz.setBizModule(auditLog.bizModule());
            biz.setBizId(bizId != null ? bizId : 0L);
            biz.setFieldName(auditLog.fieldName());
            biz.setUserName(user != null ? user.username() : "system");
            biz.setUserId(user != null ? user.userId() : 0L);
            
            // 从方法参数中提取旧值
            Object[] args = pjp.getArgs();
            if (args != null && args.length > 0) {
                // 尝试从第一个参数中获取旧值
                Object firstArg = args[0];
                if (firstArg != null) {
                    biz.setOldValue(serializeFieldValue(firstArg, auditLog.fieldName()));
                }
            }
            
            // 从返回值中提取新值
            if (result != null) {
                biz.setNewValue(serializeFieldValue(result, auditLog.fieldName()));
            }
            
            bizLogMapper.insert(biz);
            
            log.debug("业务日志记录成功, userId={}, bizModule={}, bizId={}, field={}, oldValue={}, newValue={}", 
                    user != null ? user.userId() : 0, auditLog.bizModule(), bizId, auditLog.fieldName(),
                    biz.getOldValue(), biz.getNewValue());
        } catch (Exception e) {
            log.error("保存业务日志失败, bizModule={}", auditLog.bizModule(), e);
        }
    }
    
    /**
     * 从方法返回值中提取业务ID
     * 支持多种返回类型：
     * - Long: 直接返回
     * - ApiResponse<Long>: 从data中提取
     * - ApiResponse<Map>: 从data.id中提取
     * - Map: 从id字段提取
     */
    @SuppressWarnings("unchecked")
    private Long extractBizId(Object result) {
        if (result == null) {
            return null;
        }
        
        try {
            // 1. 直接返回Long
            if (result instanceof Long) {
                return (Long) result;
            }
            
            // 2. 返回ApiResponse
            if (result.getClass().getName().contains("ApiResponse")) {
                // 使用反射获取data字段
                java.lang.reflect.Method getDataMethod = result.getClass().getMethod("data");
                Object data = getDataMethod.invoke(result);
                
                if (data == null) {
                    return null;
                }
                
                // 2.1 data是Long
                if (data instanceof Long) {
                    return (Long) data;
                }
                
                // 2.2 data是Map，提取id字段
                if (data instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    Object idObj = dataMap.get("id");
                    if (idObj instanceof Number) {
                        return ((Number) idObj).longValue();
                    }
                    if (idObj instanceof String) {
                        return Long.parseLong((String) idObj);
                    }
                }
                
                // 2.3 data是对象，尝试getId方法
                try {
                    java.lang.reflect.Method getIdMethod = data.getClass().getMethod("getId");
                    Object idObj = getIdMethod.invoke(data);
                    if (idObj instanceof Number) {
                        return ((Number) idObj).longValue();
                    }
                } catch (NoSuchMethodException e) {
                    // 没有getId方法，跳过
                    log.debug("对象没有getId方法: {}", data.getClass().getName());
                }
            }
            
            // 3. 直接返回Map
            if (result instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) result;
                Object idObj = resultMap.get("id");
                if (idObj instanceof Number) {
                    return ((Number) idObj).longValue();
                }
                if (idObj instanceof String) {
                    return Long.parseLong((String) idObj);
                }
            }
        } catch (Exception e) {
            log.debug("提取业务ID失败，返回null", e);
        }
        
        return null;
    }
    
    /**
     * 获取当前HTTP请求
     */
    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.debug("获取HTTP请求失败", e);
            return null;
        }
    }
    
    /**
     * 序列化方法返回值为JSON字符串
     */
    private String serializeResult(Object result) {
        if (result == null) {
            return null;
        }
        
        try {
            // 过滤掉不需要序列化的类型
            if (result.getClass().getName().startsWith("jakarta.servlet") || 
                result.getClass().getName().startsWith("org.springframework")) {
                return null;
            }
            
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.debug("序列化返回结果失败", e);
            return null;
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
    
    /**
     * 从对象中提取指定字段的值
     * 支持多种对象类型：
     * - 基本类型/包装类：直接序列化
     * - Map: 直接从map中获取
     * - POJO: 使用反射获取getter方法
     * - ApiResponse: 从data中提取
     */
    @SuppressWarnings("unchecked")
    private String serializeFieldValue(Object obj, String fieldName) {
        if (obj == null) {
            return null;
        }
        
        try {
            // 0. 如果是基本类型或包装类，直接返回
            if (obj instanceof Number || obj instanceof Boolean || obj instanceof Character) {
                return objectMapper.writeValueAsString(obj);
            }
            if (obj instanceof String) {
                return objectMapper.writeValueAsString(obj);
            }
            
            // 1. 如果是Map，直接获取
            if (obj instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) obj;
                Object value = map.get(fieldName);
                return value != null ? objectMapper.writeValueAsString(value) : null;
            }
            
            // 2. 如果是ApiResponse，先提取data
            if (obj.getClass().getName().contains("ApiResponse")) {
                java.lang.reflect.Method getDataMethod = obj.getClass().getMethod("data");
                Object data = getDataMethod.invoke(obj);
                if (data == null) {
                    return null;
                }
                // 递归处理data
                return serializeFieldValue(data, fieldName);
            }
            
            // 3. 如果是POJO，使用反射获取getter
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            java.lang.reflect.Method getter = obj.getClass().getMethod(getterName);
            Object value = getter.invoke(obj);
            return value != null ? objectMapper.writeValueAsString(value) : null;
            
        } catch (Exception e) {
            log.debug("提取字段值失败, fieldName={}", fieldName, e);
            return null;
        }
    }
}
