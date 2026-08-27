package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 缓存配置。
 * 通过 sky.cache.enabled 控制是否启用缓存（默认 true）：
 *  - true  : 初始化 RedisTemplate，用户端菜品查询走 Redis 缓存
 *  - false : 不初始化 Redis，菜品查询直接查数据库（用于无缓存瓶颈对比测试）
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "sky.cache.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfiguration {
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("正在初始化redis{}", redisConnectionFactory);
        RedisTemplate redisTemplate = new RedisTemplate();
        //设计链接对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //设置Rediskey的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}
