package com.sky.config;

import com.sky.interceptor.JwtTokenAdminInterceptor;
import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * Phase 7: JWT 校验已迁移至 Gateway（JwtAuthGlobalFilter），
     * 拦截器注册已移除，改为 UserContextFilter 从请求头读取用户身份。
     * 原 JwtTokenAdminInterceptor / JwtTokenUserInterceptor 保留（@Deprecated），
     * 紧急情况下可通过恢复此注释 + 注释 UserContextFilter 快速回滚。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("JWT 校验已迁移至 Gateway，拦截器已禁用");
        // Phase 7: 以下拦截器已由 Gateway JwtAuthGlobalFilter + UserContextFilter 替代
        // 恢复方法：取消注释下方代码，并注释 UserContextFilter 的 @Component
        /*
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login");
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/user/login")
                .excludePathPatterns("/user/shop/status");
        */
    }

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
