package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.cart.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.exception.BaseException;
import com.sky.feign.MenuFeignClient;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import com.sky.vo.menu.DishVO;
import com.sky.vo.menu.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private MenuFeignClient menuFeignClient;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    public void add(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        //条件查询已有的购物车清单
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList != null && !shoppingCartList.isEmpty()) { //如果已存在该目标，则物品数量+1
            ShoppingCart exShoppingCart = shoppingCartList.get(0);
            exShoppingCart.setNumber(exShoppingCart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(exShoppingCart);
        }else{ //如果目标不存在
            if(shoppingCartDTO.getDishId() != null){ // 目标为菜品 → 通过 Feign 远程获取菜品信息
                Result<DishVO> result = menuFeignClient.getDishById(shoppingCartDTO.getDishId());
                DishVO dishVO = result.getData();
                shoppingCart.setName(dishVO.getName());
                shoppingCart.setImage(dishVO.getImage());
                shoppingCart.setAmount(dishVO.getPrice());
            }else{ // 目标为套餐 → 通过 Feign 远程获取套餐信息
                Result<SetmealVO> result = menuFeignClient.getSetmealById(shoppingCartDTO.getSetmealId());
                SetmealVO setmealVO = result.getData();
                shoppingCart.setName(setmealVO.getName());
                shoppingCart.setImage(setmealVO.getImage());
                shoppingCart.setAmount(setmealVO.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 根据用户id查询购物车
     */
    public List<ShoppingCart> showShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        return shoppingCartMapper.list(shoppingCart);
    }

    /**
     * 清空购物车
     */
    public void clean() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.cleanShoppingCart(userId);
    }

    /**
     * 删除购物车中的一个商品
     * @param shoppingCartDTO
     */
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getCurrentId();
        Long dishId = shoppingCartDTO.getDishId();
        Long setmealId = shoppingCartDTO.getSetmealId();
        if(userId == null){
            throw new BaseException("无法获取用户id");
        }else if(dishId == null && setmealId == null){
            throw new BaseException("没有指定删除目标");
        }
        Map map = new HashMap();
        map.put("userId", userId);
        map.put("dishId", dishId);
        map.put("setmealId", setmealId);
        // 同菜品多口味时精确到口味，避免误删其他口味的条目
        map.put("dishFlavor", shoppingCartDTO.getDishFlavor());
        shoppingCartMapper.delBymap(map);
    }

    /**
     * 直接添加购物车条目（供 order-service 再来一单调用）
     * 实体已由调用方填充完整（name/image/amount 等）
     * @param shoppingCart 已填充完整的购物车实体
     */
    public void addEntity(ShoppingCart shoppingCart) {
        shoppingCart.setCreateTime(LocalDateTime.now());
        shoppingCartMapper.insert(shoppingCart);
    }
}
