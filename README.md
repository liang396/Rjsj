# 人间食记

人间食记是一个基于 Spring Boot 的本地生活服务项目，包含短信登录、店铺查询、缓存优化、秒杀下单、博客互动、签到统计、附近店铺检索等能力。

当前版本重点完成了两项核心改造：

- 缓存穿透由“空值缓存”思路升级为“布隆过滤器 + Redis 缓存”。
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

## 项目说明

这个仓库当前维护的是“人间食记”版本，不再保留原始 RabbitMQ 异步下单文档描述。若继续演进，建议下一步补充：

- Kafka Topic 创建说明
- Redis / Kafka / MySQL 的 Docker 启动方案
- 接口文档与压测结果
