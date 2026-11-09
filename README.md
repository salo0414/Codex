# FitFlow 健身房管理系统

Vue 3 + Spring Boot + MySQL 的全栈管理系统 MVP，包含运营总览、会员管理、课程管理和预约管理。

## 快速启动

1. 安装 Docker Desktop、Node.js 20+、Java 17+ 和 Maven。
2. 在 PowerShell 设置下方环境变量，再于项目根目录运行 `docker compose up -d`。
3. 后端：在同一个 PowerShell 窗口运行 `cd backend`、`mvn spring-boot:run`（默认端口 8080）。
4. 前端：另开终端运行 `cd frontend`、`npm install`、`npm run dev`（默认端口 5173）。

前端开发服务器会把 `/api` 请求代理到后端。MySQL 数据库名为 `fitflow`，用户名固定为 Compose 创建的 `fitflow`，密码由环境变量提供；数据库端口只绑定本机回环地址。

数据库和管理员密码必须通过环境变量提供，项目配置不保存真实密码。PowerShell 启动前可设置：

```powershell
$env:MYSQL_ROOT_PASSWORD = "请设置独立强密码"
$env:MYSQL_PASSWORD = "请设置独立强密码"
$env:DB_USERNAME = "fitflow"
$env:DB_PASSWORD = $env:MYSQL_PASSWORD
$env:ADMIN_USERNAME = "admin"
$env:ADMIN_PASSWORD = "请设置独立强密码"
docker compose up -d
```

在 IntelliJ IDEA 中，也可将这些变量配置到 Spring Boot 运行配置的“环境变量”字段。

## 在 VS Code 中运行

请用 VS Code 打开项目根目录 `E:\codex xm\project1`，不要双击 `frontend/index.html` 在浏览器中以 `file://` 打开。

1. VS Code 终端执行 `cd frontend`、`npm install`、`npm run dev`。
2. 启动后打开终端显示的 `http://localhost:5173` 地址。
3. 另开终端执行 `cd backend`、`mvn spring-boot:run`，并确保 MySQL 已启动。

也可以使用 VS Code「运行和调试」中的“FitFlow 前端（Edge）”启动前端。会员、课程等页面可以加载，但需要后端和数据库均已运行才能读写数据。
