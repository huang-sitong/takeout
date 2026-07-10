package com.sky.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sky.json.JacksonObjectMapper;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * OpenFeign 通用配置
 *
 * JacksonObjectMapper 序列化配置，
 * 确保 Feign 调用时 Java 8 时间类型 (LocalDateTime/LocalDate/LocalTime) 正确序列化。
 */
public class FeignConfig {

    @Bean
    public Decoder feignDecoder() {
        return new SpringDecoder(messageConverters());
    }

    @Bean
    public Encoder feignEncoder() {
        return new SpringEncoder(messageConverters());
    }

    private ObjectFactory<HttpMessageConverters> messageConverters() {
        ObjectMapper mapper = new JacksonObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
        return () -> new HttpMessageConverters(converter);
    }
}
