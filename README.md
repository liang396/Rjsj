# 人间食记

人间食记是一个基于 Spring Boot 的本地生活服务项目，包含短信登录、店铺查询、缓存优化、秒杀下单、博客互动、签到统计、附近店铺检索等能力。

当前版本重点完成了两项核心改造：

- 缓存穿透由“空值缓存”升级为“布隆过滤器 + Redis 缓存”。
- 异步下单由 RabbitMQ/Redis Stream 方案切换为 Kafka 消息队列。

## 技术栈

- Java 8
- Spring Boot 2.3
- MyBatis-Plus
- MySQL 8
- Redis
- Redisson
- Kafka
- Hutool
- Lombok

## 当前实现亮点

### 1. 登录与状态校验

- 基于 Redis 存储登录态，替代传统 Session 共享方案。
- 通过拦截器完成用户身份刷新与登录校验。

### 2. 店铺缓存方案

- 店铺详情查询前先经过布隆过滤器，提前拦截非法店铺 ID。
- 正常数据走 Redis 逻辑过期缓存，降低数据库压力。
- 缓存重建使用独立线程异步处理，避免热点 key 同时击穿数据库。

相关代码：

- `src/main/java/com/hmdp/config/ShopBloomFilterInitializer.java`
- `src/main/java/com/hmdp/service/impl/ShopServiceImpl.java`
- `src/main/java/com/hmdp/utils/CacheClient.java`

### 3. 秒杀与异步下单

- Redis + Lua 脚本负责秒杀资格预检，校验库存和一人一单。
- 预检通过后生成订单并投递到 Kafka Topic：`seckill.voucher.order`。
- Kafka 消费者异步创建订单并扣减数据库库存。
- Redisson 分布式锁用于兜底控制重复下单并发问题。

相关代码：

- `src/main/resources/seckill.lua`
- `src/main/java/com/hmdp/service/impl/VoucherOrderServiceImpl.java`
- `src/main/java/com/hmdp/listener/SeckillVoucherKafkaListener.java`

### 4. 其他 Redis 能力

- GEO：附近店铺检索
- ZSet：点赞排行榜
- Set：关注 / 共同关注
- Bitmap：签到统计
- HyperLogLog：UV 统计

## 运行环境

启动前请先准备以下依赖：

- MySQL
- Redis
- Kafka

默认配置位于 `src/main/resources/application.yaml`，本地示例端口如下：

- MySQL：`127.0.0.1:3306`
- Redis：`127.0.0.1:6379`
- Kafka：`localhost:9092`
- 应用端口：`8081`

## Docker 启动方案

下面是一组适合本项目本地开发的最小启动命令。

### 1. 启动 MySQL

```bash
docker run -d \
  --name renjian-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=290390 \
  -e MYSQL_DATABASE=dingping \
  mysql:8.0
```

导入初始化脚本：

```bash
docker exec -i renjian-mysql mysql -uroot -p290390 dingping < src/main/resources/db/hmdp.sql
```

### 2. 启动 Redis

```bash
docker run -d \
  --name renjian-redis \
  -p 6379:6379 \
  redis:6.2
```

### 3. 启动 Kafka

如果本地没有现成 Kafka，开发阶段可以直接用 KRaft 单机模式：

```bash
docker run -d \
  --name renjian-kafka \
  -p 9092:9092 \
  -e KAFKA_CFG_NODE_ID=1 \
  -e KAFKA_CFG_PROCESS_ROLES=broker,controller \
  -e KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT \
  -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@127.0.0.1:9093 \
  -e KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=true \
  bitnami/kafka:3.7
```

### 4. 创建订单 Topic

如果没有开启自动建 Topic，可以手动创建：

```bash
docker exec -it renjian-kafka kafka-topics.sh \
  --create \
  --topic seckill.voucher.order \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1
```

## 启动方式

```bash
mvn clean package
mvn spring-boot:run
```

或直接运行启动类：

- `src/main/java/com/hmdp/HmDianPingApplication.java`

## 数据库脚本

初始化脚本位于：

- `src/main/resources/db/hmdp.sql`

## 接口文档与压测结果

当前仓库暂未内置独立的接口文档与压测报告文件，建议后续补充以下内容：

- 基于 Apifox / Swagger / Postman 的接口说明
- 秒杀下单链路的并发压测结果
- Redis 布隆过滤器命中率与缓存命中率统计
- Kafka 消费延迟与订单落库耗时统计
