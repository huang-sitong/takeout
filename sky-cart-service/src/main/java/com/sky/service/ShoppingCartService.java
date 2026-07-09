package com.sky.service;

import com.sky.dto.cart.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    void add(ShoppingCartDTO shoppingCartDTO);

    /**
     * 查询购物车
     */
    List<ShoppingCart> showShoppingCart();

    /**
     * 清空购物车
     */
    void clean();

    /**
     * 删除购物车中的一个商品
     * @param shoppingCartDTO
     */
    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * 直接添加购物车条目（供 order-service 再来一单调用）
     * @param shoppingCart 已填充完整的购物车实体
     */
    void addEntity(ShoppingCart shoppingCart);
}
