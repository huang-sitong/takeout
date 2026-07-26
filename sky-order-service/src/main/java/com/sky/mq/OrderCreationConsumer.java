package com.sky.mq;

import com.alibaba.fastjson.JSON;
import com.sky.context.BaseContext;
import com.sky.dto.mq.OrderSubmitMessage;
import com.sky.dto.order.OrdersSubmitDTO;
import com.sky.entity.OrderAsyncTask;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.OrderAsyncTaskMapper;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 订单创建消费者（削峰用）
 *
 * 监听 order-submit-queue Topic，按可控速率消费订单创建请求。
 * 消费失败时：业务异常直接标记失败，基础设施异常抛出让 RocketMQ 重试。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "order-submit-queue",
        consumerGroup = "order-creation-consumer-group",
        selectorExpression = "*",
        consumeThreadMax = 20,
        maxReconsumeTimes = 3
)
public class OrderCreationConsumer implements RocketMQListener<MessageExt> {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderAsyncTaskMapper orderAsyncTaskMapper;

    @Override
    public void onMessage(MessageExt messageExt) {
        String msgId = messageExt.getMsgId();
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("received order creation message: msgId={}", msgId);

        // 反序列化
        OrderSubmitMessage message;
        try {
            message = JSON.parseObject(body, OrderSubmitMessage.class);
        } catch (Exception e) {
            log.error("failed to parse order submit message: msgId={}, body={}", msgId, body, e);
            return; // 消息格式错误，ACK 丢弃
        }

        String requestId = message.getRequestId();
        Long userId = message.getUserId();

        // 幂等校验：任务已完成则跳过
        OrderAsyncTask task = orderAsyncTaskMapper.getByRequestId(requestId);
        if (task != null && OrderAsyncTask.STATUS_COMPLETED.equals(task.getStatus())) {
            log.info("task already completed, skip: requestId={}", requestId);
            return;
        }

        // 反序列化订单提交 DTO
        OrdersSubmitDTO submitDTO;
        try {
            submitDTO = JSON.parseObject(message.getSubmitDTOJson(), OrdersSubmitDTO.class);
        } catch (Exception e) {
            log.error("failed to parse submitDTO: requestId={}", requestId, e);
            markFailed(requestId, task, "请求数据格式错误");
            return;
        }

        // 设置用户上下文（消费者线程没有 HTTP 请求）
        BaseContext.setCurrentId(userId);
        BaseContext.setCurrentRole("USER");

        try {
            // 执行实际订单创建逻辑
            orderService.processOrderCreation(requestId, userId, submitDTO);

            // 标记任务完成
            if (task != null) {
                task.setStatus(OrderAsyncTask.STATUS_COMPLETED);
                task.setUpdateTime(LocalDateTime.now());
                orderAsyncTaskMapper.updateCompleted(task);
            }
            log.info("order creation completed: requestId={}", requestId);

        } catch (AddressBookBusinessException | ShoppingCartBusinessException | OrderBusinessException e) {
            // 业务异常：不可重试，直接标记失败
            log.warn("business exception, marking as failed: requestId={}, error={}", requestId, e.getMessage());
            markFailed(requestId, task, e.getMessage());

        } catch (Exception e) {
            // 基础设施异常：抛出让 RocketMQ 重试
            log.error("infrastructure exception, will retry: requestId={}", requestId, e);
            throw new RuntimeException("Order creation failed, will retry", e);

        } finally {
            BaseContext.removeAll();
        }
    }

    private void markFailed(String requestId, OrderAsyncTask task, String errorMsg) {
        if (task == null) {
            task = orderAsyncTaskMapper.getByRequestId(requestId);
        }
        if (task != null) {
            task.setStatus(OrderAsyncTask.STATUS_FAILED);
            task.setErrorMsg(errorMsg);
            task.setUpdateTime(LocalDateTime.now());
            orderAsyncTaskMapper.updateFailed(task);
        }
    }
}
