package com.sky.feign;

import com.sky.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * sky-order-service OpenFeign 接口（店铺相关）
 *
 * 供 agent-service 点餐助手查询营业状态。
 * 用户身份通过 FeignInterceptor 注入 X-User-Id Header 传递（status 接口本身不依赖用户身份）。
 */
@FeignClient(name = "sky-order-service")
public interface ShopFeignClient {

    /**
     * 查询店铺营业状态
     * @return 1-营业中 0-已打烊
     */
    @GetMapping("/user/shop/status")
    Result<Integer> getShopStatus();
}
