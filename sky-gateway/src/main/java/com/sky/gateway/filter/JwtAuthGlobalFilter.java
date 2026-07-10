package com.sky.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.sky.gateway.properties.GatewayJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway 全局 JWT 认证过滤器（Phase 7）
 *
 * 统一校验所有请求的 JWT 令牌，将用户身份注入请求头传递给下游服务。
 * 替代原拦截器方式 (JwtTokenAdminInterceptor / JwtTokenUserInterceptor)，集中处理认证。
 *
 * 优先级: HIGHEST_PRECEDENCE + 1（Sentinel 限流之后，路由转发之前）
 */
@Slf4j
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private GatewayJwtProperties jwtProperties;

    /** 不需要认证的白名单路径 */
    private static final List<String> WHITELIST = Arrays.asList(
            "/admin/employee/login",
            "/user/user/login",
            "/user/shop/status",
            "/ws/"          // WebSocket 由自身处理认证
    );

    /** JWT claims 中的用户类型键 */
    private static final String USER_TYPE_ADMIN = "ADMIN";
    private static final String USER_TYPE_USER = "USER";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.debug("JwtAuthGlobalFilter processing: {}", path);

        // 白名单路径直接放行
        if (isWhitelisted(path)) {
            log.debug("Whitelist path, skipping JWT auth: {}", path);
            return chain.filter(exchange);
        }

        // 尝试提取并验证 JWT
        try {
            String token;

            // 优先以 admin 身份尝试
            if (path.startsWith("/admin/")) {
                token = extractToken(exchange, jwtProperties.getAdminTokenName());
                if (token != null) {
                    Claims claims = parseJwt(token, jwtProperties.getAdminSecretKey());
                    Long userId = Long.valueOf(claims.get("empId").toString());
                    log.debug("Admin JWT verified, empId={}", userId);
                    return forwardWithUserContext(exchange, chain, userId, USER_TYPE_ADMIN);
                } else {
                    log.warn("Admin token not found in header '{}'", jwtProperties.getAdminTokenName());
                    return unauthorized(exchange, "JWT令牌不存在");
                }
            }

            // 以 user 身份尝试
            if (path.startsWith("/user/")) {
                token = extractToken(exchange, jwtProperties.getUserTokenName());
                if (token != null) {
                    Claims claims = parseJwt(token, jwtProperties.getUserSecretKey());
                    Long userId = Long.valueOf(claims.get("userId").toString());
                    log.debug("User JWT verified, userId={}", userId);
                    return forwardWithUserContext(exchange, chain, userId, USER_TYPE_USER);
                } else {
                    log.warn("User token not found in header '{}'", jwtProperties.getUserTokenName());
                    return unauthorized(exchange, "JWT令牌不存在");
                }
            }

            // 其他路径（/notify/ 等），直接放行
            log.debug("Path without auth requirement, passing through: {}", path);
            return chain.filter(exchange);

        } catch (Exception e) {
            log.warn("JWT verification failed for path {}: {}", path, e.getMessage());
            return unauthorized(exchange, "JWT校验失败: " + e.getMessage());
        }
    }

    /**
     * 将用户身份注入请求头，转发到下游服务
     */
    private Mono<Void> forwardWithUserContext(ServerWebExchange exchange, GatewayFilterChain chain,
                                              Long userId, String role) {
        log.info("forwardWithUserContext: userId={}, role={}", userId, role);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Role", role)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        return chain.filter(mutatedExchange);
    }

    /**
     * 返回 401 未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> result = new HashMap<>(3);
        result.put("code", 0);
        result.put("msg", message);
        result.put("data", null);

        byte[] bytes = JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * 从请求头提取令牌
     */
    private String extractToken(ServerWebExchange exchange, String headerName) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        List<String> values = headers.get(headerName);
        log.debug("extractToken: name={}, values={}, allKeys={}", headerName, values, headers.keySet());
        if (values != null && !values.isEmpty()) {
            log.debug("extractToken: found value (len={})", values.get(0).length());
            return values.get(0);
        }
        return null;
    }

    /**
     * 判断路径是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        for (String whitePath : WHITELIST) {
            if (path.equals(whitePath) || path.startsWith(whitePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析 JWT（与 JwtUtil.parseJWT 实现一致）
     */
    private static Claims parseJwt(String token, String secretKey) {
        return Jwts.parser()
                .verifyWith(getKey(secretKey))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从密钥字符串派生 HMAC-SHA256 密钥（与 JwtUtil.getKey 实现一致）
     */
    private static SecretKey getKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(keyBytes);
            return new SecretKeySpec(hash, "HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Override
    public int getOrder() {
        // 在 Sentinel 限流之后执行（Sentinel 的 Order 通常是 HIGHEST_PRECEDENCE）
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
