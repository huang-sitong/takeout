package com.sky.controller.admin;

import com.sky.dto.menu.SetmealDTO;
import com.sky.dto.menu.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.menu.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminSetmealController")
@RequestMapping("/admin/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    SetmealService setmealService;

    /**
     * 保存套餐
     * @param setmealDTO
     * @return
     */
    @PostMapping()
    @CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId") //key: setmealCache::categoryId
    public Result save(@RequestBody SetmealDTO setmealDTO){
        log.info("保存套餐:{}", setmealDTO);
        setmealService.saveWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询套餐:{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping()
    @CacheEvict(cacheNames = "setmealCache", allEntries = true) //删除所有缓存
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除套餐:{}", ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据id查询套餐的详细信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("根据id查询套餐:{}", id);
        SetmealVO setmealVO = setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }

    /**
     * 根据id查询套餐的详细信息（精确路径版，避免路径变量触发全量 mapping 遍历）
     * @param id
     * @return
     */
    @GetMapping("/detail")
    public Result<SetmealVO> getByIdByParam(@RequestParam("id") Long id){
        log.info("根据id查询套餐:{}", id);
        SetmealVO setmealVO = setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }

    /**
     * 更新菜品信息
     * @param setmealDTO
     * @return
     */
    @PutMapping()
    @CacheEvict(cacheNames = "setmealCache", allEntries = true) //删除所有缓存
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("更新菜品信息:{}", setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }

    /**
     * 套餐起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true) //删除所有缓存
    public Result startOrStop(@PathVariable Integer status, Long id) {
        setmealService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 套餐起售停售（精确路径版，避免路径变量触发全量 mapping 遍历）
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true) //删除所有缓存
    public Result startOrStopByParam(@RequestParam("status") Integer status, @RequestParam("id") Long id) {
        setmealService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 根据状态统计套餐数量（供 Feign 内部调用）
     * @param status 套餐状态
     * @return 套餐数量
     */
    @GetMapping("/countByStatus")
    public Result<Integer> countByStatus(@RequestParam Integer status) {
        log.info("统计套餐状态为{}的数量", status);
        Integer count = setmealService.countByStatus(status);
        return Result.success(count);
    }
}
