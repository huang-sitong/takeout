package com.sky;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * sky-order-service 启动类 — 订单核心 + 报表 + 工作台 + 店铺管理微服务
 *
 * 依赖: cart-service / user-service / menu-service（全部通过 Feign 调用）
 * 中间件: Seata + RocketMQ + Sentinel + Redis
 */
@Slf4j
@EnableFeignClients
@EnableScheduling
@EnableDiscoveryClient
@SpringBootApplication
@EnableTransactionManagement
public class SkyOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyOrderApplication.class, args);
        log.info("sky-order-service started on port 8086");
    }
}
