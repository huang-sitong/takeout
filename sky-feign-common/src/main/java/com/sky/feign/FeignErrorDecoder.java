package com.sky.feign;

import com.sky.result.Result;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenFeign 错误解码器
 *
 * 将 Feign 调用中的 HTTP 错误状态码转换为统一的 Result 返回格式，
 * 避免下游服务异常被包装为通用的 FeignException。
 */
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String reason = response.reason();

        log.warn("Feign call failed: method={}, status={}, reason={}", methodKey, status, reason);

        return switch (status) {
            case 400 -> new RuntimeException("请求参数错误: " + reason);
            case 401 -> new RuntimeException("认证失败，请重新登录");
            case 403 -> new RuntimeException("权限不足");
            case 404 -> new RuntimeException("请求的资源不存在: " + methodKey);
            case 500 -> new RuntimeException("下游服务内部错误: " + reason);
            case 503 -> new RuntimeException("下游服务暂不可用，请稍后再试");
            default -> FeignException.errorStatus(methodKey, response);
        };
    }
}
