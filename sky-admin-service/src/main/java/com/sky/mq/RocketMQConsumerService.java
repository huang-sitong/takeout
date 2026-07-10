package com.sky.mq;

import com.alibaba.fastjson.JSON;
import com.sky.dto.mq.OrderNotificationMessage;
import com.sky.entity.OrderNotification;
import com.sky.mapper.OrderNotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * RocketMQ 消费者（管理端）
 *
 * 监听 order-notification Topic，消费订单相关通知消息：
 * - payment-success  (type=1): 支付成功，提醒商家接单
 * - reminder         (type=2): 用户催单
 * - order-submit     (type=3): 新订单通知，提醒商家有新订单
 * - order-cancel     (type=4): 订单取消
 * - order-complete   (type=5): 订单完成
 *
 * 消费逻辑：解析消息 → 幂等校验 → 写入通知表（审计日志）
 */
@Slf4j
@Service
@RocketMQMessageListener(
        topic = "order-notification",
        consumerGroup = "sky-admin-consumer-group",
        selectorExpression = "*"
)
public class RocketMQConsumerService implements RocketMQListener<MessageExt> {

    @Autowired
    private OrderNotificationMapper orderNotificationMapper;

    @Override
    public void onMessage(MessageExt messageExt) {
        String msgId = messageExt.getMsgId();
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);

        log.info("received order notification: msgId={}, body={}", msgId, body);

        // 幂等校验：同一消息不重复写入
        OrderNotification existing = orderNotificationMapper.getByMsgId(msgId);
        if (existing != null) {
            log.info("duplicate message, skip: msgId={}", msgId);
            return;
        }

        // 解析消息体
        OrderNotificationMessage messageDTO;
        try {
            messageDTO = JSON.parseObject(body, OrderNotificationMessage.class);
        } catch (Exception e) {
            log.error("failed to parse message: msgId={}, body={}", msgId, body, e);
            return;
        }

        // 写入通知表
        OrderNotification notification = OrderNotification.builder()
                .type(messageDTO.getType())
                .orderId(messageDTO.getOrderId())
                .content(messageDTO.getContent())
                .msgId(msgId)
                .createTime(LocalDateTime.now())
                .build();

        try {
            orderNotificationMapper.insert(notification);
            log.info("notification saved: msgId={}, type={}, orderId={}", msgId, messageDTO.getType(), messageDTO.getOrderId());
        } catch (Exception e) {
            log.error("failed to save notification: msgId={}", msgId, e);
        }
    }
}
