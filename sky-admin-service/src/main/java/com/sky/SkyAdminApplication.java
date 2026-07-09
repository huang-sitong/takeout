package com.sky;

import com.sky.utils.WeChatPayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * sky-admin-service 启动类 — 员工管理微服务
 *
 * 排除 sky-common 中 WeChatPayUtil（依赖 wechatpay-apiv3，本服务无需微信支付）
 */
@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
@EnableTransactionManagement
@ComponentScan(basePackages = "com.sky",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.sky\\.utils\\.WeChatPayUtil"))
public class SkyAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyAdminApplication.class, args);
        log.info("sky-admin-service started on port 8082");
    }
}
