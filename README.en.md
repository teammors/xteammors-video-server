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

AI-enhanced video generation service - Distributed multi-instance, shared connection pool

## Project Purpose

This project aims to provide a **highly available, horizontally scalable** unified video generation service platform that integrates multiple mainstream AI video generation models (Seedance, Vidu, PixVerse, Veo 3.0, HappyHorse), ensuring service stability through **distributed concurrency control** and **multi-instance shared connection pool** mechanisms, and offering a one-stop video generation solution for users.

## Feature Overview

Receive image/video URLs and text content from clients, store them in the database, and invoke third-party AI video generation services through task scheduling, supporting multiple model selections. Update the database status when the task is completed, providing query interfaces. The service supports multi-instance deployment, with instances sharing model connection pools and concurrency quotas through Redis.

## Core Features

### 1. Distributed Multi-instance Deployment
- **Horizontal scaling support**: Can run multiple service instances simultaneously
- **Automatic load balancing**: Instances compete for tasks through database
- **Stateless design**: Any instance can process tasks for any model

### 2. Multi-instance Shared Model Connection Pool
- **Redis-based distributed counting**: All instances share the same set of model connection pools
- **Atomic operation for concurrency control**: Ensure concurrency safety through Redis INCR/DECR
- **Unified connection pool management**: Each model independently configures max-concurrent, fair allocation among instances
- **Dynamic scaling without restart**: Changes take effect immediately

### 3. Dynamic Model Configuration
- Users can select which video generation model to use
- Configuration file driven, no code coupling
- Support for flexible new model expansion

### 4. Distributed Task Locking
- Implemented based on database `FOR UPDATE SKIP LOCKED`
- Avoid duplicate processing among multiple instances
- Automatic release, no manual intervention required

### 5. Graceful Shutdown
- Wait for tasks to complete when application shuts down
- Configurable waiting time
- Automatically release connection pool quotas in Redis
- Avoid interrupting ongoing tasks

### 6. Asynchronous Task Processing
- Separation of scheduling and execution
- Thread pool supports concurrent processing
- Failure retry mechanism (up to 3 times)

## Workflow

```
1. User submits task → Create task record (PENDING status)
                        ↓
2. Scheduled scheduler scans pending tasks
                        ↓
3. Acquire distributed concurrency token
                        ↓
4. Call third-party video generation API
                        ↓
5. Poll task status until completion/failure
                        ↓
6. Update task status (COMPLETED/FAILED)
                        ↓
7. Release concurrency token
```

## Technology Stack

| Component | Version | Description |
|-----------|---------|-------------|
| Java | 21 | Runtime environment |
| Spring Boot | 3.1.8 | Basic framework |
| MyBatis Plus | 3.5.8 | ORM framework |
| Redis | - | Distributed concurrency control |
| MySQL | 8.0+ | Data storage |
| Quartz | 2.3.2 | Task scheduling |

## Project Structure

```
x-message-video-service/
├── pom.xml
└── src/main/
    ├── java/com/xteam/video/
    │   ├── VideoServiceApplication.java    # Startup class
    │   ├── config/                        # Configuration classes
    │   │   ├── AsyncConfig.java           # Async thread pool config
    │   │   ├── DistributedConcurrencyManager.java  # Distributed concurrency manager
    │   │   ├── GracefulShutdownConfig.java        # Graceful shutdown config
    │   │   ├── ThirdPartyServiceConfig.java      # Third-party service config
    │   │   └── FileUploadConfig.java             # File upload config
    │   ├── controller/                    # Controller layer
    │   │   └── VideoTaskController.java   # Video task API
    │   ├── service/                       # Service layer
    │   │   ├── VideoTaskService.java     # Task service interface
    │   │   ├── impl/
    │   │   │   └── ThirdPartyServiceClientImpl.java # Third-party call implementation
    │   │   ├── ThirdPartyServiceClient.java    # Third-party call interface
    │   │   └── MessageNotificationService.java # Message notification interface
    │   ├── scheduler/                     # Task scheduler
    │   │   └── VideoTaskScheduler.java  # Scheduler processor
    │   ├── mapper/                       # Data access layer
    │   │   └── VideoTaskMapper.java
    │   ├── entity/                        # Entity classes
    │   │   └── VideoTask.java
    │   ├── enums/                        # Enum classes
    │   │   └── TaskStatus.java
    │   ├── dto/                          # Data transfer objects
    │   │   ├── request/
    │   │   │   └── VideoTaskCreateRequest.java
    │   │   └── response/
    │   ├── service/thirdparty/           # Third-party service wrappers
    │   │   ├── Seedance15VideoGenerator.java
    │   │   ├── Seedance2VideoGenerator.java
    │   │   ├── ViduVideoGenerator.java
    │   │   ├── PixVerseVideoGenerator.java
    │   │   ├── Veo3VideoGenerator.java
    │   │   └── HappyHorseVideoGenerator.java
    │   └── message/                       # Message body
    │       └── VideoTaskNotification.java
    └── resources/
        ├── application.yml                # Application config
        └── mapper/
            └── VideoTaskMapper.xml       # MyBatis mapping file
```

## API Endpoints

### Get Available Services

```
GET /api/video/services
```

Response example:
```json
{
  "code": 200,
  "data": [
    {"code": "seedance-1-5-pro", "name": "Seedance 1.5 Pro", "description": "Complex scenarios like drone flying, immersive flight experience"},
    {"code": "seedance-2-0", "name": "Seedance 2.0", "description": "Suitable for generating short videos, diverse styles, supports multiple scenarios"},
    {"code": "vidu", "name": "Vidu", "description": "Fast generation speed"},
    {"code": "pixverse", "name": "PixVerse", "description": "AI art style videos, unlimited creativity, excellent visual effects"},
    {"code": "veo-3-0", "name": "Veo 3.0", "description": "Google AI video generation, professional output"},
    {"code": "happy-horse", "name": "HappyHorse", "description": "Alibaba Cloud Bailian - Child-friendly content, safe and reliable"}
  ]
}
```

### Create Video Task

```
POST /api/video/tasks
Content-Type: application/json
```

**Request Parameters**:

| Parameter | Type | Required | Default | Description | Optional Values |
|-----------|------|----------|---------|-------------|-----------------|
| userId | String | Yes | - | User ID | Any string |
| fileUrls | List\<String\> | Yes | - | Image/video URL list | Accessible HTTP/HTTPS URLs |
| textContent | String | No | - | Text prompt content | Any text |
| service | String | No | seedance-1-5-pro | Service code | seedance-1-5-pro, seedance-2-0, vidu, pixverse, veo-3-0, happy-horse |
| generateAudio | Boolean | No | true | Whether to generate audio | true, false |
| videoRatio | String | No | 16:9 | Video aspect ratio | 16:9, 9:16, 1:1, 4:3, 3:4 |
| videoDuration | Long | No | 11 | Video duration (seconds) | Recommended 5-30, depends on model |
| showWatermark | Boolean | No | true | Whether to show watermark | true, false |
| resolution | String | No | 720p | Video resolution | 720p, 1080p, 4K |

**Response Example**:
```json
{
  "code": 200,
  "message": "Task created successfully",
  "data": {
    "id": 1234567890123456789,
    "userId": "user123",
    "filePath": "https://example.com/image.jpg",
    "textContent": "Make the picture move",
    "thirdPartyService": "seedance-1-5-pro",
    "status": "PENDING",
    "statusDesc": "Pending",
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

### Get Task Details

```
GET /api/video/tasks/{id}
```

**Response Example**:
```json
{
  "code": 200,
  "data": {
    "id": 1234567890123456789,
    "userId": "user123",
    "filePath": "https://example.com/image.jpg",
    "textContent": "Make the picture move",
    "thirdPartyService": "seedance-1-5-pro",
    "status": "COMPLETED",
    "statusDesc": "Completed",
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

### Get User's Task List

```
GET /api/video/users/{userId}/tasks
```

**Response Example**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1234567890123456789,
      "userId": "user123",
      "status": "COMPLETED",
      "statusDesc": "Completed",
      "resultUrl": "https://cdn.example.com/video/output.mp4"
    }
  ]
}
```

## Supported Video Generation Models

### 1. Seedance 1.5 Pro

**Features**:
- Suitable for complex scenarios like drone flying
- Provides immersive flight experience
- High-quality video output

**Service Code**: `seedance-1-5-pro`

**Usage Example**:
```bash
curl -X POST http://localhost:6666/api/video/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "fileUrls": ["https://www.teammors.top/files/wggirls.jpeg"],
    "textContent": "Make the person in the image smile and show charm",
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

**Features**:
- Suitable for generating short videos
- Diverse styles, supports multiple scenarios
- Fast generation speed

**Service Code**: `seedance-2-0`

**Usage Example**:
```bash
curl -X POST http://localhost:6666/api/video/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "fileUrls": ["https://www.teammors.top/files/wggirls.jpeg"],
    "textContent": "Turn the image into a beautiful anime-style short video",
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

**Features**:
- Fast generation speed
- Supports multiple sub-models
- Cost-effective

**Service Code**: `vidu`

**Supported Model Configurations** (set model-id in application.yml):
- `vidu2.0` - Latest version (default)
- `viduq3-pro-fast` - Q3 Pro Fast
- `viduq3-turbo` - Q3 Turbo (fastest)
- `viduq3-pro` - Q3 Pro
- `viduq2-pro-fast` - Q2 Pro Fast
- `viduq2-pro` - Q2 Pro
- `viduq2-turbo` - Q2 Turbo
- `viduq1` - Q1 Standard
- `viduq1-classic` - Q1 Classic

**Usage Example**:
```bash
curl -X POST http://localhost:6666/api/video/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "fileUrls": ["https://www.teammors.top/files/wggirls.jpeg"],
    "textContent": "Turn this photo into a vivid dynamic video",
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

**Features**:
- AI art style videos
- Unlimited creativity, excellent visual effects
- Supports multiple camera movement effects

**Service Code**: `pixverse`

**Supported Model Configurations** (set model-id in application.yml):
- `v6` - Latest version (recommended)
- `v5.6` - Version 5.6
- `v5.5` - Version 5.5
- `v5` - Version 5.0
- `v4.5` - Version 4.5
- `v4` - Version 4.0
- `v3.5` - Version 3.5

**Usage Example**:
```bash
curl -X POST http://localhost:6666/api/video/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "fileUrls": ["https://www.teammors.top/files/wggirls.jpeg"],
    "textContent": "Artistic stylization, dreamy color flow",
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

**Features**:
- Google AI video generation
- Professional output
- Two speed mode options

**Service Code**: `veo-3-0`

**Supported Model Configurations** (set model-id in application.yml):
- `veo3-fast` - Fast version (25 credits/time, recommended)
- `veo3` - Professional version (180 credits/time)

**Usage Example**:
```bash
curl -X POST http://localhost:6666/api/video/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "fileUrls": ["https://www.teammors.top/files/wggirls.jpeg"],
    "textContent": "High-quality cinematic video generation",
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

**Features**:
- Alibaba Cloud Bailian product
- Child-friendly content
- Safe and reliable
- Image-to-video support

**Service Code**: `happy-horse`

**Supported Model Configurations** (set model-id in application.yml):
- `happyhorse-1.0-i2v` - Image-to-video model (default)

**Usage Example**:
```bash
curl -X POST http://localhost:6666/api/video/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "fileUrls": ["https://www.teammors.top/files/wggirls.jpeg"],
    "textContent": "Make the person in the image smile and show charm",
    "service": "happy-horse",
    "generateAudio": true,
    "videoRatio": "16:9",
    "videoDuration": 11,
    "showWatermark": true,
    "resolution": "720p"
  }'
```

---

## Configuration Guide

### Complete Configuration Example

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
      description: Complex scenarios like drone flying, immersive flight experience
      api-url: https://ark.cn-beijing.volces.com/api/v3
      api-key: ${SEEDANCE_API_KEY}
      max-concurrent: 5
      model-id: doubao-seedance-1-5-pro-251215
    seedance-2-0:
      name: Seedance 2.0
      description: Suitable for generating short videos, diverse styles, supports multiple scenarios
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
      description: AI art style videos, unlimited creativity, excellent visual effects
      api-url: https://app-api.pixverseai.cn
      api-key: ${PIXVERSE_API_KEY}
      max-concurrent: 3
      model-id: v6
    veo-3-0:
      name: Veo 3.0
      description: Google AI video generation, professional output
      api-url: https://veo3api.com
      api-key: ${VEO_API_KEY}
      max-concurrent: 2
      model-id: veo3-fast
    happy-horse:
      name: HappyHorse
      description: Alibaba Cloud Bailian - Child-friendly content, safe and reliable
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

### Configuration Explanation

| Config Item | Description | Default Value |
|-------------|-------------|---------------|
| `server.port` | Service port | 6666 |
| `third-party.default-service` | Default service code | seedance-1-5-pro |
| `third-party.services.*.max-concurrent` | Maximum concurrency for service | 1 |
| `third-party.services.*.api-url` | Third-party API URL | - |
| `third-party.services.*.api-key` | Third-party API key | - |
| `third-party.services.*.model-id` | Model ID to use | - |
| `task.scheduler.enabled` | Whether to enable scheduler | true |
| `task.scheduler.cron` | Scheduler cron expression | 0/5 * * * * ? |
| `graceful-shutdown.wait-seconds` | Shutdown waiting seconds | 10 |

## Task Status Flow

```
PENDING ──scheduler triggered──→ PROCESSING ──success──→ COMPLETED
  │                            │
  │                            └──failure──→ PENDING (retry)
  │                                         │
  │                                         └──(after 3 times)──→ FAILED
```

## Distributed Concurrency Control and Shared Connection Pool Principles

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                              User Request Layer                       │
│         ┌──────────┐      ┌──────────┐      ┌──────────┐           │
│         │  User 1  │      │  User 2  │      │  User 3  │           │
│         └─────┬────┘      └─────┬────┘      └─────┬────┘           │
└───────────────┼──────────────────┼──────────────────┼────────────────┘
                │                  │                  │
┌───────────────▼──────────────────▼──────────────────▼────────────────┐
│                      Service Instance Layer (Horizontally Scalable)   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │  Instance A  │  │  Instance B  │  │  Instance C  │  │  Instance D │ │
│  │  (3 conc.)   │  │  (2 conc.)   │  │  (1 conc.)   │  │  (0 conc.)  │ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬──────┘ │
└─────────┼──────────────────┼──────────────────┼──────────────────┘
          │                  │                  │
          └──────────────────┴──────────────────┘
                                │
                    ┌───────────▼───────────┐
                    │   Redis Shared Data Layer│
                    │  ┌─────────────────┐ │
                    │  │ Shared Connection Pool Counter  │ │
                    │  └─────────────────┘ │
                    │  seedance-1-5-pro: 3/5 │
                    │  seedance-2-0: 4/5       │
                    │  vidu: 2/5               │
                    │  pixverse: 1/3          │
                    │  veo-3-0: 1/2           │
                    │  happy-horse: 2/3       │
                    └──────────────────────────┘
                                │
                    ┌───────────▼───────────┐
                    │   MySQL Database       │
                    │  ┌─────────────────┐ │
                    │  │ video_task table  │ │
                    │  └─────────────────┘ │
                    │ Distributed task lock mechanism│
                    └──────────────────────────┘
```

### Core Concept Details

#### 1. Multi-instance Shared Model Connection Pool

**Design Philosophy**:
- Not allocating connection quotas per instance, but the entire cluster shares a unified connection pool
- Each model independently configures `max-concurrent`, all instances share this quota
- Ensuring concurrency safety through Redis atomic operations

**Workflow**:
```
Instance processing task flow:
1. Acquire task → 2. Attempt to acquire Redis distributed quota → 3. Execute if successful → 4. Release quota after completion
                        ↓
                Wait if failed, retry on next scheduling
```

#### 2. Distributed Counting Mechanism

**Redis Key Structure**:
```
video:concurrent:{service_code}  → Current concurrency count
```

**Atomic Operations**:
- `INCR`: Attempt to acquire quota (+1)
- `DECR`: Release quota (-1)
- `GET`: Check current concurrency

**Quota Acquisition Logic**:
```java
if (redis.get(concurrent_key) < max_concurrent) {
    redis.incr(concurrent_key) // Atomic operation
    return true; // Acquisition successful
}
return false; // Acquisition failed
```

#### 3. Advantage Features

**High Availability**:
- Single instance failure does not affect overall service
- Other instances continue to process remaining tasks
- Redis and MySQL as shared layers ensure no data loss

**Elastic Scaling**:
- Can start/stop service instances at any time
- No reconfiguration needed when adjusting instance count
- Connection pool automatically redistributes among instances

**Load Balancing**:
- Through database `FOR UPDATE SKIP LOCKED` mechanism
- Fair task competition among instances
- Avoid some instances being overloaded while others are idle

#### 4. Multi-instance Deployment Practices

**Recommended Deployment Architecture**:
```yaml
# Production environment recommendations
- Redis: 1 master + 2 slaves + sentinel mode
- MySQL: Master-slave replication architecture
- Service instances: 3-5 (adjust based on load)
```

**Horizontal Scaling Steps**:
1. Start new service instances (configuring the same Redis and MySQL)
2. New instances automatically join task scheduling
3. Shared connection pool quotas automatically start allocating to new instances
4. No need to restart any existing services

**Scaling Down Steps**:
1. Send SIGTERM signal to the instance to stop
2. Instance enters graceful shutdown mode
3. Wait for running tasks to complete (up to 10 seconds)
4. Automatically release all occupied Redis quotas
5. Instance exits normally

## Distributed Concurrency Control Principle (Detailed)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Redis Shared Connection Pool                  │
├─────────────────────────────────────────────────────────────────────┤
│  video:concurrent:seedance-1-5-pro  →  current: 3 / max: 5           │
│  video:concurrent:seedance-2-0      →  current: 2 / max: 5           │
│  video:concurrent:vidu              →  current: 1 / max: 5           │
│  video:concurrent:pixverse          →  current: 2 / max: 3           │
│  video:concurrent:veo-3-0           →  current: 1 / max: 2           │
│  video:concurrent:happy-horse       →  current: 1 / max: 3           │
└─────────────────────────────────────────────────────────────────────┘
                          ↑
        ┌─────────────────┼─────────────────┐
        │                 │                 │
    ┌───┴───┐         ┌───┴───┐         ┌───┴───┐
    │InstanceA│         │InstanceB│         │InstanceC│
    │ 3 tasks│         │ 2 tasks│         │ 1 task │
    └───────┘         └───────┘         └───────┘
```

Multiple instances share concurrency quotas for each model, implemented through Redis INCR/DECR atomic operations.

## Getting Started

### 1. Requirements
- JDK 21+
- MySQL 8.0+
- Redis 6.0+

### 2. Database Initialization

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

### 3. Build and Deploy

```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/x-message-video-service-1.0.0.jar
```

## Message Notification

Message notification mechanism can be integrated after task completion. The current implementation outputs to logs, and you can integrate Kafka, RabbitMQ, or other message queues as needed.

---

## FAQ

### Q: How to switch sub-models for Vidu or PixVerse?
A: Find the corresponding service configuration in `application.yml` and modify the `model-id` parameter.

### Q: Task remains in PENDING status?
A: Please check:
1. Is the scheduler enabled (task.scheduler.enabled=true)?
2. Is Redis connection normal?
3. Are there other instances processing tasks?
4. Are all model concurrency quotas full?

### Q: How to adjust concurrency?
A: Modify the `max-concurrent` parameter for the corresponding service in `application.yml`, no restart required.

### Q: Does it support local file upload?
A: Current version supports fileUrls with online URLs. To support local file upload, you can extend based on FileUploadConfig.

### Q: How to deploy multiple service instances?
A: Just ensure all instances:
1. Connect to the same Redis server
2. Connect to the same MySQL database
3. Use the same `application.yml` configuration (especially service configurations)
Then start multiple instances, and they will automatically collaborate.

### Q: What are the advantages of multi-instance deployment?
A: Key advantages include:
- **High availability**: Single instance failure does not affect overall service
- **Horizontal scaling**: Adding instances improves overall processing capacity
- **Load balancing**: Tasks are automatically distributed among instances
- **Easy maintenance**: Can perform rolling updates without service interruption

### Q: Will Redis quotas leak if an instance crashes abnormally?
A: Normally no, because:
1. All quotas are automatically released during graceful shutdown
2. In case of abnormal crash, Redis connection disconnection triggers cleanup
3. In worst case, quotas will reset through timeout mechanism on next scheduling

### Q: How to monitor the operation status of each instance?
A: Can monitor through:
1. Checking concurrency counting keys in Redis
2. Querying task status distribution in MySQL
3. Monitoring log outputs from each instance
4. Integrating Prometheus + Grafana (self-development required)

### Q: What is the maximum number of instances that can run simultaneously?
A: Theoretically no hard limit, mainly depends on:
1. Connection limits of Redis and MySQL
2. Load capacity of the database
3. Network bandwidth
Production environment recommends starting with 3-5 instances.

---

## License

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
