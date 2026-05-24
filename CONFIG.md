# 配置说明

## 📁 配置文件列表

### 环境配置文件
- `application.yml` - 主配置文件(通用配置)
- `application-dev.yml` - 开发环境配置
- `application-test.yml` - 测试环境配置
- `application-prod.yml` - 生产环境配置

### 环境变量文件
- `.env.example` - 环境变量示例文件(提交到Git)
- `.env` - 实际环境变量文件(已在.gitignore中,不会提交)

---

## 🚀 快速开始

### 1. 创建环境变量文件
```bash
cd e-plating-erp-backend
cp .env.example .env
```

### 2. 编辑配置
```bash
# Windows
notepad .env

# Linux/Mac
vim .env
```

至少需要配置:
```env
# 数据库
DB_URL=jdbc:mysql://localhost:3306/e_plating_erp?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=root

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
```

### 3. 启动应用
```bash
mvn spring-boot:run
```

---

## 📧 邮件服务配置

详细配置指南请查看:
- 📖 [邮件配置完整指南](./email-configuration-guide.md)
- 🚀 [邮件配置快速入门](./email-quick-start.md)

### 快速配置(同机部署场景)

#### 方案1: 本地SMTP(推荐开发环境)
```env
MAIL_ENABLED=true
MAIL_HOST=localhost
MAIL_PORT=25
MAIL_FROM=noreply@localhost
```

#### 方案2: 第三方SMTP
```env
MAIL_ENABLED=true
MAIL_HOST=smtp.qq.com
MAIL_PORT=465
MAIL_USERNAME=your-email@qq.com
MAIL_PASSWORD=授权码
MAIL_FROM=your-email@qq.com
MAIL_SMTP_AUTH=true
MAIL_SSL_ENABLED=true
```

---

## 🔧 环境说明

### 开发环境 (dev)
- 数据库: localhost:3306
- Redis: localhost:6379
- OSS: MinIO (localhost:9000)
- 邮件: 默认禁用
- Druid监控: 启用 (http://localhost:8080/druid)

### 测试环境 (test)
- 数据库: test-mysql:3306
- Redis: test-redis:6379
- OSS: MinIO (minio:9000)
- 邮件: 默认禁用

### 生产环境 (prod)
- 数据库: 通过环境变量配置
- Redis: 通过环境变量配置
- OSS: 腾讯云/阿里云(通过环境变量)
- 邮件: 默认启用(必须配置)
- Druid监控: 禁用

---

## 📋 配置项说明

### 必需配置
| 配置项 | 说明 | 示例 |
|-------|------|------|
| DB_URL | 数据库连接URL | jdbc:mysql://localhost:3306/e_plating_erp |
| DB_USERNAME | 数据库用户名 | root |
| DB_PASSWORD | 数据库密码 | root |
| REDIS_HOST | Redis主机 | localhost |
| REDIS_PORT | Redis端口 | 6379 |

### 可选配置
| 配置项 | 说明 | 默认值 |
|-------|------|--------|
| JWT_SECRET | JWT密钥 | ChangeThisJwtSecretAtLeast32Chars! |
| OSS_PROVIDER | OSS提供商 | minio |
| MAIL_ENABLED | 邮件功能开关 | false |
| AUTHZ_CACHE_SECONDS | 权限缓存时间 | 120 |

### 邮件配置(启用邮件功能时必需)
| 配置项 | 说明 | 示例 |
|-------|------|------|
| MAIL_ENABLED | 是否启用邮件 | true |
| MAIL_HOST | SMTP服务器 | smtp.qq.com |
| MAIL_PORT | SMTP端口 | 465 |
| MAIL_USERNAME | 邮箱账号 | user@qq.com |
| MAIL_PASSWORD | 邮箱密码/授权码 | authorization-code |
| MAIL_FROM | 发件人地址 | user@qq.com |
| MAIL_LOGIN_URL | 登录页面URL | http://localhost:5173/login |

---

## 🔐 安全建议

1. **生产环境必须配置**:
   - 使用强JWT密钥(至少32字符)
   - 数据库密码使用强密码
   - 邮箱使用授权码而非登录密码
   - OSS使用独立账号,配置最小权限

2. **不要提交敏感信息**:
   - `.env` 文件已在 `.gitignore` 中
   - 使用环境变量管理敏感配置
   - 定期更换密码和密钥

3. **邮件安全**:
   - 生产环境必须使用SSL/TLS
   - 使用企业邮箱而非个人邮箱
   - 定期查看邮件发送记录

---

## 📚 相关文档

- [邮件配置完整指南](./email-configuration-guide.md) - 详细的邮件配置说明
- [邮件配置快速入门](./email-quick-start.md) - 快速上手指南
- [DEPLOYMENT.md](../DEPLOYMENT.md) - 部署文档
- [README.md](../README.md) - 项目说明

---

## ❓ 常见问题

### Q: 如何切换环境?
```bash
# 通过 spring.profiles.active 参数
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # 开发环境
mvn spring-boot:run -Dspring-boot.run.profiles=test # 测试环境
mvn spring-boot:run -Dspring-boot.run.profiles=prod # 生产环境
```

### Q: .env文件不生效?
确保:
1. 文件名为 `.env` (不是 `.env.txt`)
2. 文件在 `e-plating-erp-backend` 目录下
3. 重启应用后生效

### Q: 如何查看当前生效的配置?
启动应用后查看日志,会打印关键配置信息。

### Q: 邮件发送失败怎么办?
1. 检查 `MAIL_ENABLED=true`
2. 查看日志中的错误信息
3. 查询数据库: `SELECT * FROM msg_email_record WHERE send_status=2`
4. 参考故障排查文档

---

## 🆘 获取帮助

遇到问题时:
1. 查看应用日志
2. 检查数据库邮件记录表
3. 参考配置文档
4. 检查网络连通性
