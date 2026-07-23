package com.sky.feign;

import com.sky.context.BaseContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OpenFeign 请求拦截器
 *
 * 从 BaseContext（ThreadLocal）获取当前用户身份，注入到 Feign 请求 Header，
 * 透传到下游微服务。涵盖 HTTP 请求（UserContextFilter 设置）和
 * MQ 消费者（手动设置）两种场景。
 */
@Slf4j
@Component
public class FeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        Long userId = BaseContext.getCurrentId();
        String userRole = BaseContext.getCurrentRole();

        if (userId != null) {
            template.header("X-User-Id", String.valueOf(userId));
        }
        if (userRole != null && !userRole.isEmpty()) {
            template.header("X-User-Role", userRole);
        }
    }
}
