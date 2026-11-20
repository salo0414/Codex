# 端到端回归

## 环境准备

先启动 MySQL、Spring Boot 后端和 Vite 前端。浏览器测试默认访问 `http://localhost:5173`，后端由 Vite 代理到 `http://localhost:8080`。

第一次在 `frontend` 目录安装浏览器：

```powershell
cd frontend
npm install
npx playwright install chromium webkit
```

## 执行

```powershell
cd frontend
npm run test:e2e
```

测试会依次运行 Chromium 桌面、WebKit 桌面和 iPhone 13 模拟视口。当前覆盖登录、会员新增成功、必填与手机号格式、后端无效/超长/重复数据、模拟 500 提示、特殊字符显示、刷新和后退状态、移动视口横向溢出。测试用唯一手机号，并在结束时删除自己创建的会员。

本地管理员用户名和密码由 `ADMIN_USERNAME`、`ADMIN_PASSWORD` 决定；运行测试前设置匹配的测试环境变量：

```powershell
$env:E2E_USERNAME = "admin"
$env:E2E_PASSWORD = "你的管理员密码"
$env:E2E_BASE_URL = "http://localhost:5173"
npm run test:e2e
```

失败时 Playwright 会生成截图和 trace，保存在 `frontend/test-results`。可用 `npx playwright show-report` 查看 HTML 报告，或用 `npx playwright show-trace <trace.zip>` 检查失败步骤。

Playwright 的 Chromium 和 WebKit 是浏览器引擎测试，不能代替 Google Chrome、Apple Safari 实际版本或真机。移动网络下 LCP/CLS、4G 模拟、TLS/混合内容、部署缓存头、错误上报与回滚也需要部署环境和真实设备单独验收。
