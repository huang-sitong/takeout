package com.sky.dto.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单提交 MQ 消息体（Topic: order-submit-queue）
 *
 * 快速路径将用户下单请求序列化后发送到此 Topic，
 * 后台消费者反序列化后执行实际订单创建逻辑。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubmitMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 客户端请求幂等 ID */
    private String requestId;

    /** 下单用户 ID（消费者线程没有 HTTP 上下文，必须嵌入消息） */
    private Long userId;

    /** OrdersSubmitDTO 的 JSON 字符串 */
    private String submitDTOJson;
}
