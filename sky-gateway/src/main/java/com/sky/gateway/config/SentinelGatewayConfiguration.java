package com.sky.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Sentinel Gateway 限流配置：
 * 自定义被限流时的响应体，统一返回与 sky-server Result 结构一致的 JSON。
 * 规则通过 Nacos 数据源 (sky-gateway-flow-rules.json) 持久化下发。
 */
@Configuration
public class SentinelGatewayConfiguration {

    @PostConstruct
    public void initBlockHandler() {
        BlockRequestHandler handler = (exchange, t) -> {
            Map<String, Object> result = new HashMap<>(3);
            result.put("code", 0);
            result.put("msg", "系统繁忙，请稍后再试");
            result.put("data", null);
            return org.springframework.web.reactive.function.server.ServerResponse
                    .status(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(result);
        };
        GatewayCallbackManager.setBlockHandler(handler);
    }
}