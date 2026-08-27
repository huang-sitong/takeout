package com.sky.controller.admin;

import com.sky.dto.menu.DishDTO;
import com.sky.dto.menu.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.menu.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品相关接口
 */
@RestController("adminDishController")
@RequestMapping("/admin/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    /** 缓存未启用（sky.cache.enabled=false）时为 null，跳过缓存清理 */
    @Autowired(required = false)
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping()
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("进行新增菜品:{}", dishDTO);
        dishService.save(dishDTO);
        //删除缓存
        deletCache("dish_" + dishDTO.getCategoryId());
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("<菜品分页查询>:{}",  dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping()
    public Result delete(@RequestParam List<Long> ids){
        log.info("<删除菜品>:{}", ids);
        dishService.deleteBatch(ids);
        //删除所有缓存
        deletCache("dish_*");

        return Result.success();
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品:{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 根据id查询菜品（精确路径版，避免路径变量触发全量 mapping 遍历）
     * @param id
     * @return
     */
    @GetMapping("/detail")
    public Result<DishVO> getByIdByParam(@RequestParam("id") Long id){
        log.info("根据id查询菜品:{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 更新菜品
     * @param dishDTO
     * @return
     */
    @PutMapping()
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("更新菜品:{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        //删除所有缓存
        deletCache("dish_*");

        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    public Result<List<Dish>> list(Long categoryId){
        log.info("根据分类id查询菜品:{}", categoryId);
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    /**
     * 菜品启用/停用
     * @return
     */
    @PostMapping("status/{status}")
    public Result setDishStatus(@PathVariable Integer status, Long id){
        log.info("将菜品{}的状态设置为{}", id, status);
        dishService.setDishStatus(status, id);
        //deletCache("dish_*");
        return Result.success();
    }

    /**
     * 菜品启用/停用（精确路径版，避免路径变量触发全量 mapping 遍历）
     * @return
     */
    @PostMapping("status")
    public Result setDishStatusByParam(@RequestParam("status") Integer status, @RequestParam("id") Long id){
        log.info("将菜品{}的状态设置为{}", id, status);
        dishService.setDishStatus(status, id);
        //deletCache("dish_*");
        return Result.success();
    }

    /**
     * 根据状态统计菜品数量（供 Feign 内部调用）
     * @param status 菜品状态
     * @return 菜品数量
     */
    @GetMapping("/countByStatus")
    public Result<Integer> countByStatus(@RequestParam Integer status) {
        log.info("统计菜品状态为{}的数量", status);
        Integer count = dishService.countByStatus(status);
        return Result.success(count);
    }

    /**
     * 清理缓存数据
     * @param pattern
     */
    private void deletCache(String pattern){
        if (redisTemplate == null) {
            return; // 缓存未启用，无需清理
        }
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
