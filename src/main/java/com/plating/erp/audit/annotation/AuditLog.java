package com.plating.erp.audit.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    String module();
    String operateType();
    String bizModule() default "";
    String fieldName() default "";
    String[] maskFields() default {"password", "token", "phone"};
}
