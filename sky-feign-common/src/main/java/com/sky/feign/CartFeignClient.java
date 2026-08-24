package com.sky.feign;

import com.sky.dto.cart.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * sky-cart-service OpenFeign 接口
 *
 * 供 order-service 调用：获取购物车列表、清空购物车。
 * 用户身份通过 FeignInterceptor 注入 X-User-Id Header 传递。
 */
@FeignClient(name = "sky-cart-service")
public interface CartFeignClient {

    /**
     * 查询当前用户的购物车列表
     * @return 购物车商品列表
     */
    @GetMapping("/user/shoppingCart/list")
    Result<List<ShoppingCart>> listByUserId();

    /**
     * 添加商品到购物车（菜品/套餐二选一，可带口味；已存在同条目则数量+1）
     * 供 agent-service 点餐助手使用
     * @param shoppingCartDTO 购物车DTO
     * @return 购物车最新条目
     */
    @PostMapping("/user/shoppingCart/add")
    Result<ShoppingCart> add(@RequestBody ShoppingCartDTO shoppingCartDTO);

    /**
     * 从购物车减少一个商品（数量减1，减至0删除条目）
     * @param shoppingCartDTO 购物车DTO
     * @return
     */
    @PostMapping("/user/shoppingCart/sub")
    Result sub(@RequestBody ShoppingCartDTO shoppingCartDTO);

    /**
     * 清空当前用户的购物车
     * @return
     */
    @DeleteMapping("/user/shoppingCart/clean")
    Result cleanCart();

    /**
     * 直接添加购物车条目（供 order-service 再来一单使用）
     * @param shoppingCart 购物车实体
     * @return
     */
    @PostMapping("/user/shoppingCart/addEntity")
    Result addCartItem(@RequestBody ShoppingCart shoppingCart);
}
