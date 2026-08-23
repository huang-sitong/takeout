package com.sky.feign;

import com.sky.context.BaseContext;
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
 *
 * 兼容 AI Agent 场景：SSE 流式对话中 Spring AI 的工具执行可能发生在非 Tomcat 请求线程，
 * 此时 RequestContextHolder 无值，回退读取 BaseContext（工具方法在 Feign 调用前
 * 已将 userId set 进 BaseContext，同一调用栈内 ThreadLocal 必定有效）。
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

        if (userId == null || userId.isEmpty()) {
            // 回退：Agent 工具线程场景（无 RequestAttributes），从 BaseContext 取
            Long currentId = BaseContext.getCurrentId();
            if (currentId != null) {
                template.header("X-User-Id", String.valueOf(currentId));
                log.debug("FeignInterceptor: injected X-User-Id={} from BaseContext (no request attributes)", currentId);
            } else {
                log.debug("FeignInterceptor: no request context and empty BaseContext, skipping header injection");
            }
            return;
        }

        if (userId != null && !userId.isEmpty()) {
            template.header("X-User-Id", userId);
        }
        if (userRole != null && !userRole.isEmpty()) {
            template.header("X-User-Role", userRole);
        }
    }
}
