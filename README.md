# e-plating-erp-backend

第一阶段后端独立仓库（建议拆分后单独托管）。

## 技术栈
- Maven
- JDK 21
- Spring Boot 3
- MySQL 8 / Redis 7
- MyBatis-Plus
- Spring Security + JWT

## 启动
1. 配置 `application-dev.yml` 数据库连接
2. 执行 `mvn spring-boot:run`

## 配置管理
### 配置文件
- `application.yml` - 主配置文件
- `application-dev.yml` - 开发环境配置
- `application-prod.yml` - 生产环境配置

### 配置说明
- **数据库配置**：在 `application-*.yml` 文件中配置数据库连接信息
- **Redis配置**：在 `application-*.yml` 文件中配置Redis连接信息
- **JWT配置**：在 `application-*.yml` 文件中配置JWT密钥和过期时间
- **服务端口**：在 `application-*.yml` 文件中配置服务端口
- **API前缀**：固定为 `/api/v1`

## 说明
- API 前缀：`/api/v1`
- DB 迁移：`src/main/resources/db/migration`
- 已使用 MyBatis-Plus 持久层与分页查询，替换内存存储。
- 已启用 JWT 过滤器与 `tenant_id` 多租户强约束拦截。
- 已实现登录安全策略，包括登录尝试限制和验证码验证。
- 已实现多登录方式，包括账号密码、短信验证和扫码登录。

