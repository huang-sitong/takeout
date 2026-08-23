package com.sky.feign;

import org.springframework.context.annotation.Bean;

/**
 * Feign 客户端默认配置 —— 注册 {@link FeignInterceptor} 到每个 Feign 客户端的子上下文。
 *
 * ⚠️ 必须通过 @EnableFeignClients(defaultConfiguration = FeignInterceptorConfig.class) 引用，
 * 不能加 @Configuration/@Component 放进主容器：Spring Cloud OpenFeign (2025.0.x)
 * 构建 Feign 客户端时只从客户端子上下文收集 RequestInterceptor，主容器 Bean 不生效。
 *
 * 该类须位于组件扫描路径之外或不含 stereotype 注解，避免被 @ComponentScan("com.sky") 重复注册。
 */
public class FeignInterceptorConfig {

    @Bean
    public FeignInterceptor feignInterceptor() {
        return new FeignInterceptor();
    }
}
