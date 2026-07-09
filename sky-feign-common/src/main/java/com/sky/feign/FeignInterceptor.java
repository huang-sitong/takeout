package com.sky.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * OpenFeign 请求拦截器
 *
 * 从当前 HTTP 请求上下文获取 Gateway 注入的 X-User-Id / X-User-Role Header，
 * 透传到下游微服务，确保微服务调用链路中用户身份不丢失。
 */
@Slf4j
public class FeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.debug("FeignInterceptor: no request context, skipping header injection");
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");

        if (userId != null && !userId.isEmpty()) {
            template.header("X-User-Id", userId);
        }
        if (userRole != null && !userRole.isEmpty()) {
            template.header("X-User-Role", userRole);
        }
    }
}
