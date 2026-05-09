<div align="center">
  <img src="makey_banner.png" alt="xTeammors AI Video" width="600">
</div>

# x-teammors-video-service

![JDK](https://img.shields.io/badge/JDK-21-blue?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.8-green?style=flat-square)
![Redis](https://img.shields.io/badge/Redis-5.0%2B-red?style=flat-square)
![Protocol](https://img.shields.io/badge/Protocol-Custom%20JSON-orange?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)
![Build](https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square)

AI加强视频生成服务 - 分布式多实例、共享连接池

## 项目目的

本项目旨在提供一个**高可用、可水平扩展**的统一视频生成服务平台，集成多个主流的 AI 视频生成模型（Seedance、Vidu、PixVerse、Veo 3.0、HappyHorse），通过**分布式并发控制**和**多实例共享连接池**机制确保服务稳定性，为用户提供一站式视频生成解决方案。

## 功能概述

接收客户端上传的图片/视频 URL 和文字内容，入库后通过任务调度调用第三方 AI 视频生成服务，支持多种模型选择。任务完成后更新数据库状态，提供查询接口。服务支持多实例部署，多实例间通过 Redis 共享模型连接池与并发配额。

## 核心特性

### 1. 分布式多实例部署
- **支持水平扩展**：可同时运行多个服务实例
- **自动负载均衡**：多实例通过数据库任务竞争分配
- **无状态设计**：任意实例都可处理任意模型的任务

### 2. 多实例共享模型连接池
- **基于 Redis 的分布式计数**：所有实例共享同一套模型连接池
- **原子操作控制并发**：通过 Redis INCR/DECR 保证并发安全
- **连接池统一管理**：每个模型独立配置最大并发数，实例间公平分配
- **动态扩容无需重启**：调整配置后立即生效

### 3. 动态模型配置
- 用户可选择使用的视频生成模型
- 配置文件驱动，无代码耦合
- 支持灵活扩展新模型

### 4. 分布式任务锁
- 基于数据库 `FOR UPDATE SKIP LOCKED` 实现
- 多实例间避免重复处理
- 自动释放，无需手动干预

### 5. 优雅停机
- 应用关闭时等待任务完成
- 可配置等待时间
- 自动释放 Redis 中的连接池配额
- 避免正在执行的任务被中断

### 6. 异步任务处理
- 调度与执行分离
- 线程池支持并发处理
- 失败重试机制（最多3次）

## 工作流程

```
1. 用户提交任务 → 创建任务记录（PENDING状态）
                        ↓
2. 定时调度器扫描待处理任务
                        ↓
3. 获取分布式并发令牌
                        ↓
4. 调用第三方视频生成API
                        ↓
5. 轮询任务状态直到完成/失败
                        ↓
6. 更新任务状态（COMPLETED/FAILED）
                        ↓
7. 释放并发令牌
```

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 运行环境 |
| Spring Boot | 3.1.8 | 基础框架 |
| MyBatis Plus | 3.5.8 | ORM框架 |
| Redis | - | 分布式并发控制 |
| MySQL | 8.0+ | 数据存储 |
| Quartz | 2.3.2 | 任务调度 |

## 项目结构

```
x-message-video-service/
├── pom.xml
└── src/main/
    ├── java/com/xteam/video/
    │   ├── VideoServiceApplication.java    # 启动类
    │   ├── config/                        # 配置类
    │   │   ├── AsyncConfig.java           # 异步线程池配置
    │   │   ├── DistributedConcurrencyManager.java  # 分布式并发管理器
    │   │   ├── GracefulShutdownConfig.java        # 优雅停机配置
    │   │   ├── ThirdPartyServiceConfig.java      # 第三方服务配置
    │   │   └── FileUploadConfig.java             # 文件上传配置
    │   ├── controller/                    # 控制器层
    │   │   └── VideoTaskController.java   # 视频任务API
    │   ├── service/                       # 服务层
    │   │   ├── VideoTaskService.java     # 任务服务接口
    │   │   ├── impl/
    │   │   │   └── ThirdPartyServiceClientImpl.java # 第三方调用实现
    │   │   ├── ThirdPartyServiceClient.java    # 第三方调用接口
    │   │   └── MessageNotificationService.java # 消息通知接口
    │   ├── scheduler/                     # 任务调度
    │   │   └── VideoTaskScheduler.java  # 调度处理器
    │   ├── mapper/                       # 数据访问层
    │   │   └── VideoTaskMapper.java
    │   ├── entity/                        # 实体类
    │   │   └── VideoTask.java
    │   ├── enums/                        # 枚举类
    │   │   └── TaskStatus.java
    │   ├── dto/                          # 数据传输对象
    │   │   ├── request/
    │   │   │   └── VideoTaskCreateRequest.java
    │   │   └── response/
    │   ├── service/thirdparty/           # 第三方服务封装
    │   │   ├── Seedance15VideoGenerator.java
    │   │   ├── Seedance2VideoGenerator.java
    │   │   ├── ViduVideoGenerator.java
    │   │   ├── PixVerseVideoGenerator.java
    │   │   ├── Veo3VideoGenerator.java
    │   │   └── HappyHorseVideoGenerator.java
    │   └── message/                       # 消息体
    │       └── VideoTaskNotification.java
    └── resources/
        ├── application.yml                # 应用配置
        └── mapper/
            └── VideoTaskMapper.xml       # MyBatis映射文件
```

## API 接口

### 获取可用服务列表

```
GET /api/video/services
```

响应示例：
```json
{
  "code": 200,
  "data": [
    {"code": "seedance-1-5-pro", "name": "Seedance 1.5 Pro", "description": "无人机穿越等复杂场景，沉浸式飞行体验"},
    {"code": "seedance-2-0", "name": "Seedance 2.0", "description": "适合生成短视频，风格多样，支持多种场景"},
    {"code": "vidu", "name": "Vidu", "description": "Fast generation speed"},
    {"code": "pixverse", "name": "PixVerse", "description": "AI艺术风格视频，创意无限，视觉效果出色"},
    {"code": "veo-3-0", "name": "Veo 3.0", "description": "Google AI视频生成，专业级输出"},
    {"code": "happy-horse", "name": "HappyHorse", "description": "阿里云百炼-儿童友好内容，安全可靠"}
  ]
}
```

### 创建视频任务

```
POST /api/video/tasks
Content-Type: application/json
```

**请求参数说明**：

| 参数 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|------|------|------|--------|------|--------|
| userId | String | 是 | - | 用户ID | 任意字符串 |
| fileUrls | List\<String\> | 是 | - | 图片/视频URL列表 | 可访问的HTTP/HTTPS URL |
| textContent | String | 否 | - | 文字提示内容 | 任意文本 |
| service | String | 否 | seedance-1-5-pro | 服务代码 | seedance-1-5-pro, seedance-2-0, vidu, pixverse, veo-3-0, happy-horse |
| generateAudio | Boolean | 否 | true | 是否生成音频 | true, false |
| videoRatio | String | 否 | 16:9 | 视频比例 | 16:9, 9:16, 1:1, 4:3, 3:4 |
| videoDuration | Long | 否 | 11 | 视频时长（秒） | 建议 5-30，具体取决于模型 |
| showWatermark | Boolean | 否 | true | 是否显示水印 | true, false |
| resolution | String | 否 | 720p | 视频分辨率 | 720p, 1080p, 4K |

**响应示例**：
```json
{
  "code": 200,
  "message": "任务创建成功",
  "data": {
    "id": 1234567890123456789,
    "userId": "user123",
    "filePath": "https://example.com/image.jpg",
    "textContent": "让画面动起来",
    "thirdPartyService": "seedance-1-5-pro",
    "status": "PENDING",
    "statusDesc": "等待处理",
    "generateAudio": true,
    "videoRatio": "16:9",
    "videoDuration": 11,
    "showWatermark": true,
    "resolution": "720p",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
}
```

### 查询任务详情

```
GET /api/video/tasks/{id}
```

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "id": 1234567890123456789,
    "userId": "user123",
    "filePath": "https://example.com/image.jpg",
    "textContent": "让画面动起来",
    "thirdPartyService": "seedance-1-5-pro",
    "status": "COMPLETED",
    "statusDesc": "已完成",
    "resultUrl": "https://cdn.example.com/video/output.mp4",
    "generateAudio": true,
    "videoRatio": "16:9",
    "videoDuration": 11,
    "showWatermark": true,
    "resolution": "720p",
    "retryCount": 0,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:05:00"
  }
}
```

### 查询用户任务列表

```
GET /api/video/users/{userId}/tasks
```

**响应示例**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1234567890123456789,
      "userId": "user123",
      "status": "COMPLETED",
      "statusDesc": "已完成",
      "resultUrl": "https://cdn.example.com/video/output.mp4"
    }
  ]
}
```

## 支持的视频生成模型

### 1. Seedance 1.5 Pro

**特点**：
- 适合无人机穿越等复杂场景
- 提供沉浸式飞行体验
- 高质量视频输出

**服务代码**：`seedance-1-5-pro`

**使用示例**：
```bash
curl -X POST http://localhost:6666/api/video/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "fileUrls": ["https://www.teammors.top/files/wggirls.jpeg"],
    "textContent": "把图片中的人物做成微笑的展示风情",
    "service": "seedance-1-5-pro",
    "generateAudio": true,
    "videoRatio": "16:9",
    "videoDuration": 11,
    "showWatermark": true,
    "resolution": "720p"
  }'
```

---

### 2. Seedance 2.0

**特点**：
- 适合生成短视频
- 风格多样，支持多种场景
- 生成速度快

**服务代码**：`seedance-2-0`

**使用示例**：
```bash
curl -X POST http://localhost:6666/api/video/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "fileUrls": ["https://www.teammors.top/files/wggirls.jpeg"],
    "textContent": "让图片变成唯美动画风格的短视频",
    "service": "seedance-2-0",
    "generateAudio": true,
    "videoRatio": "9:16",
    "videoDuration": 8,
    "showWatermark": true,
    "resolution": "720p"
  }'
```

---

### 3. Vidu

**特点**：
- 快速生成速度
- 支持多种子模型
- 高性价比

**服务代码**：`vidu`

**支持的模型配置**（在 application.yml 中设置 model-id）：
- `vidu2.0` - 最新版本（默认）
- `viduq3-pro-fast` - Q3 Pro 快速版
- `viduq3-turbo` - Q3 涡轮版（最快）
- `viduq3-pro` - Q3 Pro 专业版
- `viduq2-pro-fast` - Q2 Pro 快速版
- `viduq2-pro` - Q2 Pro 专业版
- `viduq2-turbo` - Q2 涡轮版
- `viduq1` - Q1 标准版
- `viduq1-classic` - Q1 经典版

**使用示例**：
```bash
curl -X POST http://localhost:6666/api/video/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "fileUrls": ["https://www.teammors.top/files/wggirls.jpeg"],
    "textContent": "把这张照片变成生动的动态视频",
    "service": "vidu",
    "generateAudio": true,
    "videoRatio": "16:9",
    "videoDuration": 10,
    "showWatermark": false,
    "resolution": "1080p"
  }'
```

---

### 4. PixVerse

**特点**：
- AI 艺术风格视频
- 创意无限，视觉效果出色
- 支持多种运镜效果

**服务代码**：`pixverse`

**支持的模型配置**（在 application.yml 中设置 model-id）：
- `v6` - 最新版本（推荐）
- `v5.6` - 5.6 版本
- `v5.5` - 5.5 版本
- `v5` - 5.0 版本
- `v4.5` - 4.5 版本
- `v4` - 4.0 版本
- `v3.5` - 3.5 版本

**使用示例**：
```bash
curl -X POST http://localhost:6666/api/video/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "fileUrls": ["https://www.teammors.top/files/wggirls.jpeg"],
    "textContent": "艺术风格化，梦幻色彩流动",
    "service": "pixverse",
    "generateAudio": true,
    "videoRatio": "16:9",
    "videoDuration": 5,
    "showWatermark": true,
    "resolution": "720p"
  }'
```

---

### 5. Veo 3.0

**特点**：
- Google AI 视频生成
- 专业级输出
- 两种速度模式可选

**服务代码**：`veo-3-0`

**支持的模型配置**（在 application.yml 中设置 model-id）：
- `veo3-fast` - 快速版（25积分/次，推荐）
- `veo3` - 专业版（180积分/次）

**使用示例**：
```bash
curl -X POST http://localhost:6666/api/video/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "fileUrls": ["https://www.teammors.top/files/wggirls.jpeg"],
    "textContent": "高质量电影级视频生成",
    "service": "veo-3-0",
    "generateAudio": true,
    "videoRatio": "16:9",
    "videoDuration": 12,
    "showWatermark": true,
    "resolution": "1080p"
  }'
```

---

### 6. HappyHorse

**特点**：
- 阿里云百炼出品
- 儿童友好内容
- 安全可靠
- 图生视频支持

**服务代码**：`happy-horse`

**支持的模型配置**（在 application.yml 中设置 model-id）：
- `happyhorse-1.0-i2v` - 图生视频模型（默认）

**使用示例**：
```bash
curl -X POST http://localhost:6666/api/video/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "fileUrls": ["https://www.teammors.top/files/wggirls.jpeg"],
    "textContent": "把图片中的人物做成微笑的展示风情",
    "service": "happy-horse",
    "generateAudio": true,
    "videoRatio": "16:9",
    "videoDuration": 11,
    "showWatermark": true,
    "resolution": "720p"
  }'
```

---

## 配置说明

### 完整配置示例

```yaml
server:
  port: 6666
  shutdown: graceful

spring:
  application:
    name: xteammors-video-service
  lifecycle:
    timeout-per-shutdown-phase: 10s
  datasource:
    url: jdbc:mysql://localhost:3306/xteam_videos?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowMultiQueries=true
    username: root
    password: "your_password"
  servlet:
    multipart:
      enabled: true
      max-file-size: 100MB
      max-request-size: 100MB
  data:
    redis:
      host: localhost
      port: 6379
      database: 10

third-party:
  default-service: seedance-1-5-pro
  services:
    seedance-1-5-pro:
      name: Seedance 1.5 Pro
      description: 无人机穿越等复杂场景，沉浸式飞行体验
      api-url: https://ark.cn-beijing.volces.com/api/v3
      api-key: ${SEEDANCE_API_KEY}
      max-concurrent: 5
      model-id: doubao-seedance-1-5-pro-251215
    seedance-2-0:
      name: Seedance 2.0
      description: 适合生成短视频，风格多样，支持多种场景
      api-url: https://ark.cn-beijing.volces.com/api/v3
      api-key: ${SEEDANCE_API_KEY}
      max-concurrent: 5
      model-id: doubao-seedance-2-0-260128
    vidu:
      name: Vidu
      description: Fast generation speed
      api-url: https://api.vidu.com/ent/v2/img2video
      api-key: ${VIDU_API_KEY}
      max-concurrent: 5
      model-id: vidu2.0
    pixverse:
      name: PixVerse
      description: AI艺术风格视频，创意无限，视觉效果出色
      api-url: https://app-api.pixverseai.cn
      api-key: ${PIXVERSE_API_KEY}
      max-concurrent: 3
      model-id: v6
    veo-3-0:
      name: Veo 3.0
      description: Google AI视频生成，专业级输出
      api-url: https://veo3api.com
      api-key: ${VEO_API_KEY}
      max-concurrent: 2
      model-id: veo3-fast
    happy-horse:
      name: HappyHorse
      description: 阿里云百炼-儿童友好内容，安全可靠
      api-url: https://dashscope.aliyuncs.com
      api-key: ${HAPPYHORSE_API_KEY}
      max-concurrent: 3
      model-id: happyhorse-1.0-i2v

task:
  scheduler:
    enabled: true
    cron: "0/5 * * * * ?"

graceful-shutdown:
  wait-seconds: 10
```

### 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 服务端口 | 6666 |
| `third-party.default-service` | 默认服务代码 | seedance-1-5-pro |
| `third-party.services.*.max-concurrent` | 服务的最大并发数 | 1 |
| `third-party.services.*.api-url` | 第三方API地址 | - |
| `third-party.services.*.api-key` | 第三方API密钥 | - |
| `third-party.services.*.model-id` | 使用的模型ID | - |
| `task.scheduler.enabled` | 是否启用调度器 | true |
| `task.scheduler.cron` | 调度Cron表达式 | 0/5 * * * * ? |
| `graceful-shutdown.wait-seconds` | 停机等待秒数 | 10 |

## 任务状态流转

```
PENDING ──调度触发──→ PROCESSING ──成功──→ COMPLETED
  │                       │
  │                       └──失败──→ PENDING (重试)
  │                                    │
  │                                    └──(3次后)──→ FAILED
```

## 分布式并发控制原理

```
┌─────────────────────────────────────────────────────────┐
│                    Redis 共享计数器                     │
├─────────────────────────────────────────────────────────┤
│  video:concurrent:seedance-1-5-pro  →  3/5            │
│  video:concurrent:vidu              →  1/5            │
│  video:concurrent:pixverse          →  2/3            │
│  video:concurrent:veo-3-0           →  1/2            │
│  video:concurrent:happy-horse       →  2/3            │
└─────────────────────────────────────────────────────────┘
                          ↑
        ┌─────────────────┼─────────────────┐
        │                 │                 │
    ┌───┴───┐         ┌───┴───┐         ┌───┴───┐
    │实例A  │         │实例B  │         │实例E  │
    │ 3并发 │         │ 2并发 │         │ 1并发 │
    └───────┘         └───────┘         └───────┘
```

多实例共享每个模型的并发配额，通过 Redis INCR/DECR 原子操作实现。

## 运行方式

### 1. 环境要求
- JDK 21+
- MySQL 8.0+
- Redis 6.0+

### 2. 数据库初始化

```sql
CREATE DATABASE xteam_videos;

CREATE TABLE video_task (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    file_path VARCHAR(512),
    file_type VARCHAR(16),
    text_content TEXT,
    third_party_service VARCHAR(32),
    status VARCHAR(16),
    result_url VARCHAR(512),
    error_message TEXT,
    retry_count INT DEFAULT 0,
    generate_audio BOOLEAN DEFAULT TRUE,
    video_ratio VARCHAR(10),
    video_duration BIGINT,
    show_watermark BOOLEAN DEFAULT TRUE,
    resolution VARCHAR(10),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

### 3. 构建部署

```bash
# 编译
mvn clean package -DskipTests

# 运行
java -jar target/x-message-video-service-1.0.0.jar
```

## 消息通知

任务完成后可集成消息通知机制，当前实现输出到日志，您可根据需求集成 Kafka、RabbitMQ 等消息队列。

---

## 常见问题

### Q: 如何切换 Vidu 或 PixVerse 的子模型？
A: 在 `application.yml` 中找到对应服务的配置，修改 `model-id` 参数即可。

### Q: 任务一直处于 PENDING 状态？
A: 请检查：
1. 调度器是否正常启动（task.scheduler.enabled=true）
2. Redis 连接是否正常
3. 是否有其他实例正在处理任务
4. 所有模型的并发配额是否都已满

### Q: 如何调整并发数？
A: 在 `application.yml` 中修改对应服务的 `max-concurrent` 参数，无需重启即可生效。

### Q: 支持本地文件上传吗？
A: 当前版本支持 fileUrls 传入在线 URL，如需支持本地文件上传，可以基于 FileUploadConfig 扩展。

### Q: 如何部署多个服务实例？
A: 只需确保所有实例：
1. 连接同一个 Redis 服务器
2. 连接同一个 MySQL 数据库
3. 使用相同的 `application.yml` 配置（特别是服务配置）
然后启动多个实例即可，它们会自动协作。

### Q: 多实例部署有什么优势？
A: 主要优势包括：
- **高可用性**：单个实例故障不影响整体服务
- **水平扩展**：增加实例可提高整体处理能力
- **负载均衡**：任务自动在实例间分配
- **维护便利**：可以滚动更新而不中断服务

### Q: 如果某个实例异常退出，会泄漏 Redis 配额吗？
A: 正常情况下不会，因为：
1. 优雅停机时会自动释放所有配额
2. 如果是异常崩溃，Redis 的连接断开会触发清理
3. 最坏情况下，配额在下次调度时会通过超时机制重置

### Q: 如何监控各个实例的运行状态？
A: 可以通过以下方式监控：
1. 查看 Redis 中的并发计数键
2. 查询 MySQL 中的任务状态分布
3. 监控各个实例的日志输出
4. 集成 Prometheus + Grafana（需要自行开发）

### Q: 最大可以支持多少个实例同时运行？
A: 理论上没有硬性限制，主要取决于：
1. Redis 和 MySQL 的连接数限制
2. 数据库的负载能力
3. 网络带宽
生产环境建议从 3-5 个实例开始测试。

---

## 开源协议

MIT License

Copyright (c) 2024 XTeammors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
