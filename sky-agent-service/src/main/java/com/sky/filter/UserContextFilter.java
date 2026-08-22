package com.sky.filter;

import com.sky.context.BaseContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 用户上下文过滤器
 *
 * 从 Gateway 注入的 X-User-Id / X-User-Role 请求头读取用户身份，
 * 填充到 BaseContext（ThreadLocal）。
 *
 * Phase B 中 Agent 工具经 Feign 调用下游服务时，
 * FeignInterceptor 会将身份 Header 继续透传，保证"代表用户操作"链路完整。
 */
@Slf4j
@Component
@Order(1)
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        try {
            String userIdStr = request.getHeader("X-User-Id");
            String userRole = request.getHeader("X-User-Role");

            if (userIdStr != null && !userIdStr.isEmpty()) {
                Long userId = Long.valueOf(userIdStr);
                BaseContext.setCurrentId(userId);
                log.debug("UserContextFilter set currentId={}, role={}", userId, userRole);
            }

            if (userRole != null && !userRole.isEmpty()) {
                BaseContext.setCurrentRole(userRole);
            }

            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            BaseContext.removeAll();
        }
    }
}
