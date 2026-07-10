package com.sky.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消息消费者服务
 * <p>
 * 消费 order-notification 主题的订单通知消息（支付成功 + 催单）。
 * 当前阶段仅做日志记录；后续可扩展为管理端实时推送、短信通知、邮件通知等。
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "order-notification",
    consumerGroup = "sky-order-consumer-group",
    selectorExpression = "*"
)
public class RocketMQConsumerService implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        try {
            JSONObject json = JSON.parseObject(message);
            Integer type = json.getInteger("type");
            Long orderId = json.getLong("orderId");
            String content = json.getString("content");

            String typeDesc = type == 1 ? "支付成功通知" : "催单通知";
            log.info("收到订单通知消息 [{}] | orderId={} | content={}", typeDesc, orderId, content);

            // TODO: 后续可扩展为以下能力：
            //   - WebSocket/SSE 推送通知到管理端
            //   - 短信/邮件通知商家
            //   - 写入消息表供管理端轮询
            //   - 触发其他业务流程（如订单超时监控）
        } catch (Exception e) {
            log.error("订单通知消息解析失败: {}", message, e);
        }
    }
}
