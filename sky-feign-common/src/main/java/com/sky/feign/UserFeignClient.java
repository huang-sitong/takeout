package com.sky.feign;

import com.sky.entity.AddressBook;
import com.sky.entity.User;
import com.sky.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

/**
 * sky-user-service OpenFeign 接口
 *
 * 供 order-service 等下游服务调用，获取用户和地址信息。
 */
@FeignClient(name = "sky-user-service")
public interface UserFeignClient {

    /**
     * 根据id查询用户信息
     * @param id 用户id
     * @return 用户实体
     */
    @GetMapping("/user/user/detail")
    Result<User> getUserById(@RequestParam("id") Long id);

    /**
     * 根据id查询地址信息
     * @param id 地址id
     * @return 地址实体
     */
    @GetMapping("/user/addressBook/detail")
    Result<AddressBook> getAddressById(@RequestParam("id") Long id);

    /**
     * 根据日期统计用户数量
     * @param begin 开始时间（可选）
     * @param end 结束时间（可选）
     * @return 用户数量
     */
    @GetMapping("/user/user/countByDates")
    Result<Integer> countUserByDates(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime begin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end);
}
