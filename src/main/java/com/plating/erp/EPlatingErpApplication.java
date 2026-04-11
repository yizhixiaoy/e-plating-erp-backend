package com.plating.erp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
        "com.plating.erp.**.mapper",
        "com.plating.erp.common.security" // PermissionMapper 等安全侧 @Mapper
})
public class EPlatingErpApplication {
    public static void main(String[] args) {
        SpringApplication.run(EPlatingErpApplication.class, args);
    }
}
