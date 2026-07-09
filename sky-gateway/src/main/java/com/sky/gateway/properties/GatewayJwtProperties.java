package com.sky.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Gateway 端 JWT 配置（从 application.yml 或 Nacos Config 读取）
 * 与 sky-server 端 sky.jtx 配置结构一致，确保密钥互通
 */
@Component
@ConfigurationProperties(prefix = "sky.jwt")
@Data
public class GatewayJwtProperties {

    /**
     * 管理端员工生成 jwt令牌相关配置
     */
    private String adminSecretKey;
    private long adminTtl;
    private String adminTokenName;

    /**
     * 用户端微信用户生成 jwt令牌相关配置
     */
    private String userSecretKey;
    private long userTtl;
    private String userTokenName;

}
