package com.sky.dto.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单通知消息体（RocketMQ Topic: order-notification）
 *
 * type 与 Tag 的对应关系：
 *   1 → payment-success   支付成功，提醒接单
 *   2 → reminder          用户催单
 *   3 → order-submit      新订单通知
 *   4 → order-cancel      订单取消
 *   5 → order-complete    订单完成
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderNotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 通知类型：1=支付成功 2=催单 3=新订单 4=取消 5=完成 */
    private Integer type;

    /** 订单 ID */
    private Long orderId;

    /** 通知内容（可直接展示） */
    private String content;

    /** 消息发送时间戳（ISO-8601） */
    private String timestamp;
}
