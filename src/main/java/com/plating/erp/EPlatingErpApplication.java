package com.plating.erp;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan(
        basePackages = {
                "com.plating.erp.**.mapper",
                "com.plating.erp.common.security"
        },
        annotationClass = Mapper.class
)
public class EPlatingErpApplication {
    public static void main(String[] args) {
        SpringApplication.run(EPlatingErpApplication.class, args);
    }
}
