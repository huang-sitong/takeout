package com.sky.service;

import com.sky.dto.user.UserLoginDTO;
import com.sky.entity.User;

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
}
