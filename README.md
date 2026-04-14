# 基于区块链的时间币服务系统

## 一、项目结构（你需要开几个终端）

| 角色 | 目录 | 说明 |
|------|------|------|
| **后端 API** | `backend/timecoin-web` | Spring Boot，默认端口 **8080** |
| **PC 管理端** | `vue-system/pc` | 浏览器访问，默认端口 **7000** |
| **移动端 H5** | `vue-system/phone` | 手机浏览器访问，端口以终端为准（常见 **7001**） |

**建议顺序：** 先启动后端 → 再启动 PC 或手机端（可同时开）。

---

## 二、环境要求

| 组件 | 说明 |
|------|------|
| **JDK** | 11（与 `backend` 中 Maven 配置一致） |
| **Node.js** | 16+ / 18+（用于前端与 Hardhat） |
| **MySQL** | **可选**。本地未配 MySQL 时，默认使用 **H2 文件库**（数据在 `backend/timecoin-web/data/`，**重启后端后账号仍会保留**） |

项目内已附带 Maven：`tools/apache-maven-3.9.6/`（Windows 用 `mvn.cmd`；Linux / macOS 可改用 `./tools/apache-maven-3.9.6/bin/mvn`）。

---

## 三、启动后端（必须先做）

`application.properties` 中已设置 `spring.profiles.active=dev`，默认走 **`application-dev.properties`**，使用 **H2 文件数据库**（路径为项目下 `backend/timecoin-web/data/timecoin.*`），**停止再启动后端，已注册用户仍可登录**。若需要**从零清空库**，请关闭后端后删除整个 `backend/timecoin-web/data` 目录再启动。

在 **PowerShell** 中执行：

```powershell
cd backend\timecoin-web
..\..\tools\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

成功标志：日志中出现 `Tomcat started on port(s): 8080` 与 `Started TimecoinWebApplication`。  
停止：在该终端按 `Ctrl+C`。

API 根地址：**http://localhost:8080**

---

## 四、启动 PC 端（管理端）

**新开一个终端**，执行：

```powershell
cd vue-system\pc
npm install
npm run serve
```

- 浏览器打开终端里提示的地址，一般为：**http://localhost:7000**
- 前端请求后端的地址在 `vue-system/pc/src/utils/request.js` 中配置，默认 **http://localhost:8080**

**说明：** PC 端为**管理员**注册/登录与后台管理；老人、志愿者请在**移动端**使用。

---

## 五、启动移动端（老人 / 志愿者 H5）

**再开一个终端**，执行：

```powershell
cd vue-system\phone
npm install
npm run serve
```

- 本机浏览器访问：以终端输出为准，常见为 **http://localhost:7001/**
- 手机与电脑在同一 Wi‑Fi 时，用手机浏览器访问：**http://你的电脑局域网IP:端口/**（端口与终端一致）

**重要：** 移动端 `vue-system/phone/src/utils/request.js` 里 `baseURL` 默认为 `http://localhost:8080`。  
在**真机**上访问时，`localhost` 指向手机自己，会导致无法连后端。请改为电脑的局域网 IP，例如：

```javascript
baseURL: 'http://192.168.1.100:8080'  // 换成你电脑的实际 IP
```

改完后保存并重新执行 `npm run serve`。

---

## 六、使用 MySQL（本机已安装 MySQL 时）

**核心原则：** 注释掉 `spring.profiles.active=dev`，后端才会用本文件里的 MySQL 连接；否则一直走 H2。

1. **启动 MySQL 服务**（Windows 可在「服务」里确认 MySQL 正在运行）。

2. **初始化库表（二选一）**

   - **推荐（省事）：** 项目已配置在**未使用 `dev` 时**启动自动执行 `timecoin-web/src/main/resources/schema-mysql.sql`，并在连接串中带 `createDatabaseIfNotExist=true`（库不存在会自动建库）。你只要改好 `application.properties` 里的 **用户名、密码**，直接启动后端即可；**第一次启动**会自动建表并插入默认管理员 `admin` / `123456`。

   - **手动：** 仍可在本机执行仓库根目录下的 `backend/schema-timecoin.sql`（与自动脚本等价、略不同写法），用命令行或 Workbench 执行均可。

3. **改后端配置**  
   编辑 `backend/timecoin-web/src/main/resources/application.properties`：

   - **注释或删除**这一行（必须，否则仍使用 H2）：

     ```properties
     # spring.profiles.active=dev
     ```

   - 按你本机修改 **用户名、密码**（连接 URL 若与仓库默认一致可不改，已含时区等参数）：

     ```properties
     spring.datasource.username=root
     spring.datasource.password=你的MySQL密码
     ```

4. **启动后端**

   ```powershell
   cd backend\timecoin-web
   ..\..\tools\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
   ```

   若仍报 `Access denied`，说明用户名或密码与 MySQL 不一致，请改 `application.properties` 后再启动。

5. **切回 H2 开发**（可选）：恢复 `spring.profiles.active=dev` 并注释掉或忽略 MySQL 段即可。

---

## 七、区块链合约（可选）

与业务后台无强制依赖；仅演示链上合约时，在项目根目录：

```powershell
cd TimeCoinSystem
npm install
npx hardhat node
```

另开终端部署到本地节点：

```powershell
cd TimeCoinSystem
npx hardhat deploy --network localhost
```

测试网部署需在 `TimeCoinSystem` 下配置 `.env`（参考 `TimeCoinSystem/.env.example`）。

---

## 八、登录与角色说明

| 端 | 角色 | 说明 |
|----|------|------|
| **PC** | 管理员 | 管理员注册/登录与后台操作 |
| **移动端 H5** | 老人、志愿者 | 注册时选择类型；登录使用账号密码（与注册身份一致） |

管理员若未通过注册页创建，可在数据库中维护 `user` 表及 `administrator` 表对应数据。

---

## 九、常见问题

### 端口 8080 已被占用

```powershell
netstat -ano | findstr :8080
taskkill /PID <上一步最后一列的PID> /F
```

若修改后端端口，需同步修改各前端 `request.js` 中的 `baseURL`。

### 注册/登录提示「操作失败」或数据库连接错误

- 使用默认 **dev（H2）** 时，确认 `application.properties` 中存在 `spring.profiles.active=dev`，且启动命令未意外覆盖 profile。
- 使用 **MySQL** 时，确认账号密码、库名与 `schema-timecoin.sql` 已导入。

### 移动端能打开页面但接口失败

检查 `vue-system/phone/src/utils/request.js` 的 `baseURL`：真机必须用**电脑局域网 IP + 8080**，不能用 `localhost`。

---

## 十、目录说明

| 目录 | 说明 |
|------|------|
| `backend/` | Spring Boot + MyBatis 后端 |
| `vue-system/pc/` | PC 端 Vue 管理前端 |
| `vue-system/phone/` | 移动端 Vue H5（老人/志愿者） |
| `TimeCoinSystem/` | Hardhat + Solidity 合约 |
| `tools/apache-maven-3.9.6/` | 捆绑的 Maven |
