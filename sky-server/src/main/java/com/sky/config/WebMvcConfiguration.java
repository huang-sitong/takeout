package com.sky.config;

import com.sky.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Phase 7: JWT 校验已迁移至 Gateway（JwtAuthGlobalFilter），
 * 用户身份通过 UserContextFilter 从请求头 X-User-Id / X-User-Role 读取。
 * 原拦截器 JwtTokenAdminInterceptor / JwtTokenUserInterceptor 已删除。
 */
@Configuration
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("进行静态资源映射...");
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器...");
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new JacksonObjectMapper());
        converters.add(0, converter);
    }
}
