package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单通知记录表（审计日志）
 *
 * 纯后端数据存储，不暴露 REST API。
 * 消费 RocketMQ 订单通知消息后写入，供后续审计、统计使用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 通知类型：1=支付成功 2=催单 3=新订单 4=取消 5=完成 */
    private Integer type;

    /** 关联订单 ID */
    private Long orderId;

    /** 通知内容 */
    private String content;

    /** RocketMQ 消息 ID（用于幂等校验） */
    private String msgId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
