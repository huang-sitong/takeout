package com.sky;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * sky-menu-service 启动类 — 菜品/套餐/分类管理 + OSS 上传微服务
 */
@Slf4j
@EnableCaching
@EnableDiscoveryClient
@SpringBootApplication
@EnableTransactionManagement
@ComponentScan(basePackages = "com.sky")
public class SkyMenuApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyMenuApplication.class, args);
        log.info("sky-menu-service started on port 8084");
    }
}
