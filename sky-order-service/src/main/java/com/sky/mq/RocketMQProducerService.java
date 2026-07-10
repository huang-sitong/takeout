package com.sky.mq;

import com.alibaba.fastjson.JSON;
import com.sky.dto.mq.OrderNotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * RocketMQ 消息生产者服务
 * <p>
 * 替代原 WebSocket 推送，将订单状态变更通知发送到 RocketMQ。
 * 使用 asyncSend 异步发送，不阻塞主流程；消息发送失败仅记日志，不抛出异常。
 */
@Slf4j
@Component
public class RocketMQProducerService {

    private static final String TOPIC = "order-notification";
    private static final String TAG_PAYMENT_SUCCESS = "payment-success";
    private static final String TAG_REMINDER = "reminder";
    private static final String TAG_ORDER_SUBMIT = "order-submit";
    private static final String TAG_ORDER_CANCEL = "order-cancel";
    private static final String TAG_ORDER_COMPLETE = "order-complete";

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 异步发送订单通知消息
     *
     * @param orderId 订单 ID
     * @param type    通知类型：1=支付成功，2=催单，3=新订单，4=订单取消，5=订单完成
     * @param content 通知内容
     */
    public void sendOrderNotification(Long orderId, Integer type, String content) {
        String tag;
        switch (type) {
            case 1:  tag = TAG_PAYMENT_SUCCESS; break;
            case 3:  tag = TAG_ORDER_SUBMIT;    break;
            case 4:  tag = TAG_ORDER_CANCEL;    break;
            case 5:  tag = TAG_ORDER_COMPLETE;  break;
            default: tag = TAG_REMINDER;        break;
        }
        String destination = TOPIC + ":" + tag;

        OrderNotificationMessage messageDTO = OrderNotificationMessage.builder()
                .type(type)
                .orderId(orderId)
                .content(content)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        String json = JSON.toJSONString(messageDTO);
        Message<String> message = MessageBuilder.withPayload(json).build();

        log.info("async sending order notification: destination={}, orderId={}, type={}", destination, orderId, type);
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("order notification sent: destination={}, msgId={}", destination, sendResult.getMsgId());
            }

            @Override
            public void onException(Throwable e) {
                log.error("order notification send failed: destination={}, payload={}", destination, json, e);
            }
        });
    }

    /**
     * 发送新订单通知（type=3, tag=order-submit）
     *
     * @param orderId     订单 ID
     * @param orderNumber 订单号
     * @param amount      订单金额
     */
    public void sendNewOrderNotification(Long orderId, String orderNumber, String amount) {
        String content = "新订单，订单号：" + orderNumber + "，金额：" + amount + "元";
        sendOrderNotification(orderId, 3, content);
    }

    /**
     * 发送订单取消通知（type=4, tag=order-cancel）
     *
     * @param orderId  订单 ID
     * @param reason   取消原因
     * @param operator 操作方：用户/管理员
     */
    public void sendOrderCancelNotification(Long orderId, String reason, String operator) {
        String content = operator + "取消订单，原因：" + reason;
        sendOrderNotification(orderId, 4, content);
    }

    /**
     * 发送订单完成通知（type=5, tag=order-complete）
     *
     * @param orderId     订单 ID
     * @param orderNumber 订单号
     */
    public void sendOrderCompleteNotification(Long orderId, String orderNumber) {
        String content = "订单已完成，订单号：" + orderNumber;
        sendOrderNotification(orderId, 5, content);
    }
}
