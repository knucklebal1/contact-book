# contact-book

极限编程课程通讯录项目。仓库采用前后端分离结构：

```text
contact-book/
├── backend/   Spring Boot 后端
├── frontend/  React + Vite 前端
├── docs/      接口和数据库文档
└── README.md
```

## 后端运行

环境要求：JDK 17、Maven 3.9+。

```powershell
cd backend
mvn spring-boot:run
```

默认使用本地 H2 文件数据库，无需安装数据库：

```text
data/contact-book.mv.db
```

访问地址：

- 健康检查：http://localhost:8080/api/health
- Swagger UI：http://localhost:8080/swagger-ui.html

## 使用 MySQL

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="contact_book"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的密码"
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

建表参考脚本位于 `backend/src/main/resources/db/schema.sql`。

## 测试和构建

```powershell
cd backend
mvn test
mvn clean package
```

接口文档见 [docs/api.md](docs/api.md)，数据库设计见 [docs/database.md](docs/database.md)。

## 前端运行

另开终端，先确保后端运行在 `http://localhost:8080`，然后：

```powershell
cd frontend
npm install
npm run dev
```

访问终端给出的本地地址（默认 `http://127.0.0.1:5173/`）。开发服务器将 `/api` 转发到后端。前端的构建和测试命令为 `npm run build`、`npm test`。详细说明见 [frontend/README.md](frontend/README.md)。
