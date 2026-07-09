package com.sky.mq;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * RocketMQ 消息生产者服务
 * <p>
 * 替代原 WebSocket 推送，将订单状态变更通知发送到 RocketMQ。
 * 消息发送失败仅记日志，不抛出异常，不阻断主流程。
 */
@Slf4j
@Component
public class RocketMQProducerService {

    private static final String TOPIC = "order-notification";
    private static final String TAG_PAYMENT_SUCCESS = "payment-success";
    private static final String TAG_REMINDER = "reminder";

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 发送订单通知消息
     *
     * @param orderId 订单 ID
     * @param type    通知类型：1=支付成功，2=催单
     * @param content 通知内容
     */
    public void sendOrderNotification(Long orderId, Integer type, String content) {
        String tag = type == 1 ? TAG_PAYMENT_SUCCESS : TAG_REMINDER;
        String destination = TOPIC + ":" + tag;

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("orderId", orderId);
        payload.put("content", content);
        payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        String json = JSON.toJSONString(payload);
        Message<String> message = MessageBuilder.withPayload(json).build();

        try {
            SendResult result = rocketMQTemplate.syncSend(destination, message, 3000);
            log.info("RocketMQ 消息发送成功: destination={}, msgId={}, payload={}", destination, result.getMsgId(), json);
        } catch (Exception e) {
            log.error("RocketMQ 消息发送失败: destination={}, payload={}", destination, json, e);
            // 不抛异常，消息发送失败不阻断主流程
        }
    }
}
