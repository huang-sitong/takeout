package com.sky.mq;

import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * RocketMQ 广播消费者（WebSocket 推送）
 *
 * 监听 order-notification Topic，广播模式消费：
 * - 每个 admin-service 实例都会收到消息
 * - 收到后推送给当前实例持有的所有 WebSocket 客户端
 *
 * 与 RocketMQConsumerService（集群消费，写 DB）互不影响
 */
@Slf4j
@Service
@RocketMQMessageListener(
        topic = "order-notification",
        consumerGroup = "admin-ws-broadcast-group",
        selectorExpression = "*",
        messageModel = MessageModel.BROADCASTING,
        instanceName = "admin-ws-broadcast-instance"
)
public class RocketMQWebSocketConsumer implements RocketMQListener<String> {

    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    public void onMessage(String body) {
        log.info("WebSocket consumer received: {}", body);
        try {
            webSocketServer.sendToAllClient(body);
        } catch (Exception e) {
            log.error("WebSocket 推送异常: {}", body, e);
        }
    }
}
