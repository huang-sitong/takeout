package com.sky.feign;

import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.vo.menu.DishVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * sky-menu-service OpenFeign 接口
 *
 * 供 cart-service / order-service 等下游服务调用，获取菜品和套餐信息。
 */
@FeignClient(name = "sky-menu-service")
public interface MenuFeignClient {

    /**
     * 根据id查询菜品（含口味）
     * @param id 菜品id
     * @return 菜品VO
     */
    @GetMapping("/admin/dish/{id}")
    Result<DishVO> getDishById(@PathVariable("id") Long id);

    /**
     * 根据id查询套餐（含关联菜品）
     * @param id 套餐id
     * @return 套餐VO
     */
    @GetMapping("/admin/setmeal/{id}")
    Result<com.sky.vo.menu.SetmealVO> getSetmealById(@PathVariable("id") Long id);

    /**
     * 根据分类id查询菜品列表
     * @param categoryId 分类id
     * @return 菜品列表
     */
    @GetMapping("/admin/dish/list")
    Result<List<Dish>> listDishByCategory(@RequestParam("categoryId") Long categoryId);
}
