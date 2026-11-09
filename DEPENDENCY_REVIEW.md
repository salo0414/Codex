# 依赖许可证与安全扫描记录

扫描日期：2026-11-09。记录仅反映本次锁文件和 Maven 解析出的运行时依赖；发布前应重新扫描。

## 字体

已移除 Google Fonts 的远程导入。页面不再下载或打包 DM Sans、Manrope、Noto Sans SC 字体文件，改用系统 sans-serif 字体栈。这样避免字体文件授权和第三方字体请求，并减少请求与样式表体积。Google Fonts 官方说明字体授权因字体而异，需逐个核对；当前项目不再依赖这些字体。

## 前端许可证

扫描来源：`frontend/package-lock.json` 中的 npm 元数据。直接运行依赖 Vue 3.5.45（MIT）与 lucide-vue-next 0.468.0（ISC）；构建依赖 Vite 8.3.6 和 `@vitejs/plugin-vue` 6.0.10（MIT）。锁文件中的传递依赖许可证包括 MIT、ISC、Apache-2.0、BSD-2-Clause、BSD-3-Clause、MPL-2.0。MPL-2.0 依赖按其许可证要求使用和分发；完整逐项元数据以锁文件为准。

## 后端许可证

使用 `license-maven-plugin:2.5.0:add-third-party` 解析后端依赖，逐项结果保存在 [backend/THIRD-PARTY.txt](backend/THIRD-PARTY.txt)。结果包括 Apache-2.0、MIT、BSD、EPL、EDL、LGPL、MPL 以及 MySQL Connector/J 的 GPLv2 + Universal FOSS Exception。商业分发前需按逐项报告核对许可证义务，特别是 MySQL Connector/J 例外条款及 LGPL/MPL 文件级义务。

## 漏洞扫描

- `npm audit --registry=https://registry.npmjs.org`：0 个已知漏洞（61 个依赖节点）。
- OWASP Dependency-Check 13.0.0 无法更新 NVD 数据库：该扫描器要求 NVD API key，本机未配置，因此**不能将其结果视为通过**。
- 使用 OSV 官方 API 检查 Maven 运行时依赖后，发现当前 Spring Boot 3.5.16 BOM 解析出的若干版本有公告匹配：Log4j API 2.24.3、Jackson 2.21.4、Tomcat 10.1.55、Spring Framework 6.2.19。公告匹配需结合受影响范围、调用方式和部署条件进行人工定级；本次没有足够证据将这些项全部认定为可利用漏洞。升级到可用的 Java 17 兼容维护版本并重新扫描是后续修复项。

## 人工复核

- 检查业务 API 映射：仅保留仪表盘、会员、课程、教练、预约和认证接口，没有发现测试/演示路由。
- 业务 API 由认证过滤器要求有效服务端会话；登录后轮换会话 ID；退出会使会话失效。
- 未发现动态 SQL 拼接。预约容量检查与写入在事务中执行。
- 数据库用户名、数据库密码和管理员密码均改为必填环境变量；管理员用户名仍默认为 `admin`。部署环境必须设置独立强密码，并通过 HTTPS 提供服务。
