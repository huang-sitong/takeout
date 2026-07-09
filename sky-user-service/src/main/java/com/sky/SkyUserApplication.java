package com.sky;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * sky-user-service 启动类 — 用户管理 + 地址簿微服务
 */
@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
@EnableTransactionManagement
public class SkyUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyUserApplication.class, args);
        log.info("sky-user-service started on port 8083");
    }
}
