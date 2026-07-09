package com.sky.service;

import com.sky.dto.user.UserLoginDTO;
import com.sky.entity.User;

import java.util.Map;

public interface UserService {
    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);

    /**
     * 根据id查询用户
     * @param id
     * @return
     */
    User getById(Long id);

    /**
     * 根据日期统计用户数量（供 Feign 内部调用）
     * @param map 包含 begin / end (LocalDateTime)
     * @return 用户数量
     */
    Integer countByDates(Map<String, Object> map);
}
