# e-plating-erp-backend

第一阶段后端独立仓库（建议拆分后单独托管）。

## 技术栈
- Maven
- JDK 21
- Spring Boot 3
- MySQL 8 / Redis 7

## 启动
1. 配置 `application-dev.yml` 数据库连接
2. 执行 `mvn spring-boot:run`

## 说明
- API 前缀：`/api/v1`
- DB 迁移：`src/main/resources/db/migration`
- 已使用 MyBatis-Plus 持久层与分页查询，替换内存存储。
- 已启用 JWT 过滤器与 `tenant_id` 多租户强约束拦截。
