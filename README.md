# IdeaForum

面向开发者的内容社区。当前已实现账号注册/登录、个人资料和密码维护、文章浏览/搜索/分类筛选、文章草稿与发布、评论、点赞和收藏，以及个人内容动态；关注与管理能力仍在后续迭代中。项目使用独立品牌与视觉设计，不复用其他平台的品牌素材或内容。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3、Spring Security、MyBatis-Plus |
| 前端 | Vue 3、TypeScript、Vite、Vue Router、Pinia |
| 数据库 | MySQL 8 |
| API 文档 | Springdoc OpenAPI |
| 构建工具 | Maven、npm |

## 目录结构

```text
.
├── backend/       # Spring Boot REST API
├── frontend/      # Vue 3 + TypeScript SPA
├── sql/           # MySQL schema、初始化数据和迁移脚本
└── docs/          # 项目技术设计
```

## 环境要求

- JDK 17
- Maven 3.9+
- Node.js 20.19+ 与 npm
- MySQL 8

## 初始化数据库

按顺序执行根目录 `sql/` 下的脚本：

```powershell
cmd /c "mysql -u root -p < sql\001_schema.sql"
cmd /c "mysql -u root -p < sql\002_seed_data.sql"
```

`001_schema.sql` 会创建 `dev_community` 数据库和业务表，`002_seed_data.sql` 写入分类及标签。实际用户密码不会存放在 SQL 中。

## 启动后端

在 PowerShell 窗口中执行：

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/dev_community?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:DB_USERNAME = 'root'
$securePassword = Read-Host '请输入本机 MySQL 密码' -AsSecureString
$env:DB_PASSWORD = (New-Object System.Net.NetworkCredential('', $securePassword)).Password
Set-Location backend
mvn spring-boot:run
```

服务默认监听 `http://localhost:8080`。API 文档：`http://localhost:8080/swagger-ui/index.html`。
`DB_URL`、`DB_USERNAME` 和 `DB_PASSWORD` 必须在启动后端的同一个 PowerShell 窗口中设置；配置文件不提供默认凭据，环境变量在关闭该窗口后失效。登录通过服务端 Session Cookie 维持，默认有效期 12 小时；本地单机 Session 在后端重启后失效。部署到生产环境前应使用受控的生产数据库凭据，并通过 HTTPS 启用安全 Cookie 配置。

头像上传支持 JPEG 和 PNG，单个文件最大 5 MB。文件保存在 `application.yml` 中 `app.upload.avatar-directory` 指定的本机目录（默认 `backend/uploads/avatars`），不会写入数据库；备份或迁移数据库时也需要备份此目录。该目录已加入 `.gitignore`，不要提交用户上传的文件。

## 启动前端

另开 PowerShell 窗口：

```powershell
Set-Location frontend
npm install
npm run dev
```

前端默认地址为 `http://localhost:5173`。Vite 会将 `/api` 请求代理到 `http://localhost:8080`；自定义代理目标可设置 `API_PROXY_TARGET`。可参考 [frontend/.env.example](frontend/.env.example)。

## 已实现 API

| 方法 | 路径 | 权限 | 用途 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/auth/csrf` | 公开 | 获取写操作所需的 CSRF token |
| `POST` | `/api/v1/auth/register` | 公开 | 注册并建立登录 Session |
| `POST` | `/api/v1/auth/login` | 公开 | 登录 |
| `POST` | `/api/v1/auth/logout` | 登录 | 注销当前 Session |
| `GET` | `/api/v1/users/me` | 登录 | 查看当前用户 |
| `PUT` | `/api/v1/users/me` | 登录 | 修改昵称、头像 URL 和个人简介 |
| `POST` | `/api/v1/users/me/avatar` | 登录 | 上传并更新本地头像 |
| `PUT` | `/api/v1/users/me/password` | 登录 | 验证当前密码后修改密码 |
| `GET` | `/api/v1/users/me/articles` | 登录 | 分页查看自己的草稿和已发布文章 |
| `GET` | `/api/v1/users/me/likes` | 登录 | 分页查看点赞文章 |
| `GET` | `/api/v1/users/me/bookmarks` | 登录 | 分页查看收藏文章 |
| `GET` | `/api/v1/users/me/comments` | 登录 | 分页查看评论及对应文章 |
| `GET` | `/api/v1/categories` | 公开 | 获取启用的分类 |
| `GET` | `/api/v1/articles` | 公开 | 分页浏览、关键词搜索和分类筛选 |
| `GET` | `/api/v1/search` | 公开 | 文章关键词搜索 |
| `GET` | `/api/v1/articles/{id}` | 公开/作者 | 查看已发布文章或本人草稿 |
| `POST` | `/api/v1/articles` | 登录 | 创建草稿或发布文章 |
| `PUT` | `/api/v1/articles/{id}` | 作者/管理员 | 修改文章 |
| `DELETE` | `/api/v1/articles/{id}` | 作者/管理员 | 归档文章 |
| `GET` | `/api/v1/articles/{id}/comments` | 公开 | 查看评论 |
| `POST` | `/api/v1/articles/{id}/comments` | 登录 | 发表评论 |
| `GET` | `/api/v1/articles/{id}/interactions` | 公开 | 查看互动状态与计数 |
| `PUT/DELETE` | `/api/v1/articles/{id}/like` | 登录 | 点赞/取消点赞 |
| `PUT/DELETE` | `/api/v1/articles/{id}/bookmark` | 登录 | 收藏/取消收藏 |
| `GET` | `/api/v1/uploads/avatars/{filename}` | 公开 | 查看已上传头像 |

登录成功后，浏览器通过 HttpOnly Session Cookie 访问受保护接口；所有写操作需同时携带 CSRF token。密码通过 BCrypt 哈希存储；注册不允许赋予管理员角色。

## 检查构建

```powershell
Set-Location backend
mvn test
Set-Location ..\frontend
npm run build
```

## 文档与后续范围

- [技术设计文档](docs/TECHNICAL_DESIGN.md)：架构、数据模型、安全、API 和迭代边界。

后续可在现有基础上补充关注、管理后台和通知。本项目借鉴开发者社区的通用功能模式，不复制掘金或其他网站的代码、文章、图像、商标或独特页面素材。
