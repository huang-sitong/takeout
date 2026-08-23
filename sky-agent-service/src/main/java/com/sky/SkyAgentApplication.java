package com.sky;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * sky-agent-service 启动类 — AI 智能助手微服务
 *
 * 基于 Spring AI (OpenAI 兼容接口) 提供智能对话能力：
 * - Phase A: 普通对话 (/user/agent/chat 同步 + /user/agent/chat/stream SSE 流式)
 * - Phase B: Function Calling 工具集，Agent 代表用户调用 user/menu/cart/order 服务
 *
 * LLM 接入配置在 Nacos spring.ai.openai.* (base-url/api-key 经 ${ENV_VAR} 占位符注入)
 */
@Slf4j
@EnableFeignClients(defaultConfiguration = com.sky.feign.FeignInterceptorConfig.class)
@EnableDiscoveryClient
@SpringBootApplication
@ComponentScan(basePackages = "com.sky")
public class SkyAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyAgentApplication.class, args);
        log.info("sky-agent-service started on port 8087");
    }
}
