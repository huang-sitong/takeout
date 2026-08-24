package com.sky;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * sky-cart-service 启动类 — 购物车微服务
 *
 * 依赖 sky-menu-service（通过 MenuFeignClient 获取菜品/套餐信息）
 */
@Slf4j
@EnableFeignClients(defaultConfiguration = com.sky.feign.FeignInterceptorConfig.class)
@EnableDiscoveryClient
@SpringBootApplication
@EnableTransactionManagement
@ComponentScan(basePackages = "com.sky")
public class SkyCartApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyCartApplication.class, args);
        log.info("sky-cart-service started on port 8085");
    }
}
