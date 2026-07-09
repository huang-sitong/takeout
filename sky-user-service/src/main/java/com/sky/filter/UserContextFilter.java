package com.sky.filter;

import com.sky.context.BaseContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 用户上下文过滤器（Phase 7）
 *
 * 从 Gateway 注入的 X-User-Id / X-User-Role 请求头读取用户身份，
 * 填充到 BaseContext（ThreadLocal），供 Service 层通过 BaseContext.getCurrentId() 使用。
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
