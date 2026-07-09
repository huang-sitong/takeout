package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.dto.user.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    //微信服务接口地址
    public static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    public User wxLogin(UserLoginDTO userLoginDTO){
        //获取openid
        String openid = getOpenid(userLoginDTO.getCode());
        if(openid == null){
            throw new LoginFailedException("openid为空");
        }

        User user = userMapper.getByOpenid(openid);
        //如果用户是新用户
        if(user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        return user;
    }

    /**
     * 根据id查询用户
     * @param id
     * @return
     */
    public User getById(Long id) {
        return userMapper.getById(id);
    }

    /**
     * 根据日期统计用户数量（供 Feign 内部调用）
     * @param map 包含 begin / end (LocalDateTime)
     * @return 用户数量
     */
    public Integer countByDates(Map<String, Object> map) {
        return userMapper.countUserByDates(map);
    }

    /**
     * 调用微信接口服务
     * @param code
     * @return
     */
    private String getOpenid(String code){
        //微信登录请求参数
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        //发送请求
        String json = HttpClientUtil.doGet(WX_LOGIN_URL, map);
        JSONObject jsonObject = JSONObject.parseObject(json);

        return jsonObject.getString("openid");
    }
}
