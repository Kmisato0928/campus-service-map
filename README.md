# 长安大学校园服务地图系统

基于 **Java + JavaFX + WebView（Leaflet.js + OpenStreetMap）+ MySQL** 的校园地理信息服务系统，提供长安大学渭水校区的建筑浏览、搜索定位、用户收藏评论与建筑信息编辑等功能。

## 功能概览

- **地图浏览** — Leaflet 地图渲染 OpenStreetMap 瓦片，鼠标缩放拖拽，建筑按类型分色标记
- **建筑搜索** — 关键字模糊搜索 + 分类下拉筛选，结果点击自动定位
- **建筑详情** — 查看建筑信息、收藏、发表/删除评论、评论点赞
- **建筑编辑** — 管理员编辑全局生效，普通用户编辑仅自己可见，管理员可锁定建筑
- **用户系统** — 登录注册、个人中心（我的收藏、我的评论）

## 技术栈

| 层 | 技术 |
|------|------|
| 语言 | Java 17 |
| UI 框架 | JavaFX 17.0.2（含 WebView） |
| 地图引擎 | Leaflet 1.9.4 + OpenStreetMap |
| 数据库 | MySQL 8.0+ |
| 构建工具 | Maven |
| 第三方库 | JBCrypt（密码加密）、JGraphT（图算法）、SLF4J（日志） |

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+

### 1. 初始化数据库

```bash
mysql -u root -p < docs/database/init-database.sql
```

### 2. 配置数据库连接

编辑 `src/main/resources/config/database.properties`，修改用户名和密码。

### 3. 编译运行

```bash
mvn clean compile
mvn javafx:run
```

如果遇到 JavaFX 模块问题，请确认 JDK 版本为 17+ 且 JavaFX 依赖已正确下载。

### 4. 打包发布

参见 `docs/软件打包说明.md`，支持两种方式：

- **app-image 便携目录**（推荐）— 包含 JDK + JavaFX 运行时，压缩即用
- **exe 安装包** — 使用 jpackage + WiX 生成 Windows 安装程序

> 当前版本依赖 MySQL 8.0+，目标机器需要 MySQL 服务正常运行。

## 文档导航

`docs/` 目录下包含以下文档，建议按顺序阅读：

| 文档 | 内容 | 适合谁 |
|------|------|--------|
| [项目使用说明](docs/项目使用说明.md) | 环境要求、启动步骤、功能说明、设计模式一览 | **所有读者**，从这里开始 |
| [项目实现状态](docs/项目实现状态.md) | 已完成/待实现功能、数据库表结构、用户操作流程、项目结构树 | **想了解项目细节**的读者 |
| [软件打包说明](docs/软件打包说明.md) | 打包为便携目录或安装程序的详细步骤、常见问题、答辩交付清单 | **需要部署/演示**的读者 |

此外，`docs/database/` 目录下包含数据库相关脚本：

- `init-database.sql` — 一体化初始化脚本（建表 + 示例数据，**推荐执行**）
- `schema.sql` — 仅建表脚本
- `sample-data.sql` — 仅示例数据脚本

## 设计模式

| 模式 | 位置 | 应用场景 |
|------|------|---------|
| 单例模式 (Singleton) | `util/DatabaseConnection.java` | 全局唯一数据库连接 |
| 单例模式 (Singleton) | `util/UserSession.java` | 用户登录会话管理 |
| 工厂模式 (Factory) | `pattern/factory/BuildingMarkerFactory.java` | 按建筑分类创建不同颜色地图标记 |
| 模板方法模式 (Template Method) | `dao/BaseDAO.java` | 通用 CRUD 操作骨架 |
| 观察者模式 (Observer) | `controller/MapController.java` | 建筑选中事件通知详情面板更新 |

## 项目结构

```text
campus-map-system/
├── pom.xml                         # Maven 构建配置
├── src/main/java/edu/chd/campusmap/
│   ├── Main.java                   # 程序入口
│   ├── model/                      # 数据模型
│   ├── dao/                        # 数据访问层
│   ├── service/                    # 业务逻辑层
│   ├── controller/                 # 控制器层
│   ├── view/                       # JavaFX 视图层
│   ├── util/                       # 工具类
│   └── pattern/factory/            # 工厂模式实现
├── src/main/resources/
│   ├── map/                        # Leaflet 地图资源
│   ├── css/                        # 样式
│   └── config/                     # 配置文件
└── docs/
    ├── 项目使用说明.md              # 运行与使用说明
    ├── 项目实现状态.md              # 功能清单与实现状态
    ├── 软件打包说明.md              # 打包发布指南
    ├── database/                    # 数据库初始化脚本
    └── uml/                        # UML 设计图
```

## 课程信息

- **课程**：面向对象设计与模式
- **选题**：2. 长安大学校园服务地图系统
- **技术要点**：Java + JavaFX + Leaflet/OSM + MySQL，应用多种设计模式
