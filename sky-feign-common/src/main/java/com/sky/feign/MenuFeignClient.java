package com.sky.feign;

import com.sky.entity.Category;
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
    @GetMapping("/admin/dish/detail")
    Result<DishVO> getDishById(@RequestParam("id") Long id);

    /**
     * 根据id查询套餐（含关联菜品）
     * @param id 套餐id
     * @return 套餐VO
     */
    @GetMapping("/admin/setmeal/detail")
    Result<com.sky.vo.menu.SetmealVO> getSetmealById(@RequestParam("id") Long id);

    /**
     * 根据分类id查询菜品列表
     * @param categoryId 分类id
     * @return 菜品列表
     */
    @GetMapping("/admin/dish/list")
    Result<List<Dish>> listDishByCategory(@RequestParam("categoryId") Long categoryId);

    /**
     * 查询全部分类（用户端接口，type 可空：1菜品分类 2套餐分类）
     * 供 agent-service 点餐助手使用
     * @param type 分类类型，可空
     * @return 分类列表
     */
    @GetMapping("/user/category/list")
    Result<List<Category>> listCategories(@RequestParam(value = "type", required = false) Integer type);

    /**
     * 根据分类id查询【起售中】菜品列表（用户端接口，含口味，走 Redis 缓存）
     * 供 agent-service 点餐助手使用；与 /admin/dish/list 不同，只返回起售菜品
     * @param categoryId 分类id
     * @return 菜品VO列表
     */
    @GetMapping("/user/dish/list")
    Result<List<DishVO>> listSellingDishByCategory(@RequestParam("categoryId") Long categoryId);

    /**
     * 根据分类id查询【起售中】套餐列表（用户端接口）
     * @param categoryId 分类id
     * @return 套餐列表
     */
    @GetMapping("/user/setmeal/list")
    Result<List<Setmeal>> listSellingSetmealByCategory(@RequestParam("categoryId") Long categoryId);

    /**
     * 根据状态统计菜品数量（供 order-service workspace/report 调用）
     * @param status 菜品状态
     * @return 菜品数量
     */
    @GetMapping("/admin/dish/countByStatus")
    Result<Integer> countDishByStatus(@RequestParam("status") Integer status);

    /**
     * 根据状态统计套餐数量（供 order-service workspace 调用）
     * @param status 套餐状态
     * @return 套餐数量
     */
    @GetMapping("/admin/setmeal/countByStatus")
    Result<Integer> countSetmealByStatus(@RequestParam("status") Integer status);
}
