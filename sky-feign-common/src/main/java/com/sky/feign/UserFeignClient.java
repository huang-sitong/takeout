package com.sky.feign;

import com.sky.entity.AddressBook;
import com.sky.entity.User;
import com.sky.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
    @GetMapping("/user/user/{id}")
    Result<User> getUserById(@PathVariable("id") Long id);

    /**
     * 根据id查询地址信息
     * @param id 地址id
     * @return 地址实体
     */
    @GetMapping("/user/addressBook/{id}")
    Result<AddressBook> getAddressById(@PathVariable("id") Long id);
}
