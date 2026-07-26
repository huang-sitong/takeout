package com.sky.controller.user;

import com.alibaba.fastjson.JSON;
import com.sky.dto.mq.OrderSubmitMessage;
import com.sky.dto.order.OrdersPaymentDTO;
import com.sky.dto.order.OrdersSubmitDTO;
import com.sky.entity.OrderAsyncTask;
import com.sky.mapper.OrderAsyncTaskMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.SnowflakeUtil;
import com.sky.vo.order.OrderAsyncStatusVO;
import com.sky.vo.order.OrderPaymentVO;
import com.sky.vo.order.OrderSubmitVO;
import com.sky.vo.order.OrderVO;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.sky.context.BaseContext;
import com.sky.handler.SentinelBlockHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderAsyncTaskMapper orderAsyncTaskMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private static final String SUBMIT_QUEUE_TOPIC = "order-submit-queue";

    /**
     * 用户提交订单（异步削峰）
     *
     * 快速路径：只写 task 表 + 发 MQ，不执行业务逻辑。
     * 后台消费者 OrderCreationConsumer 按速率消费并创建实际订单。
     *
     * @param ordersSubmitDTO 包含 requestId（客户端生成的 UUID）
     * @return 异步任务状态
     */
    @PostMapping("/submit")
    @SentinelResource(value = "submitOrder", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "submitOrder")
    public Result<OrderAsyncStatusVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        String requestId = ordersSubmitDTO.getRequestId();
        if (requestId == null || requestId.isEmpty()) {
            return Result.error("requestId 不能为空");
        }
        Long userId = BaseContext.getCurrentId();

        log.info("async order submit: requestId={}, userId={}", requestId, userId);

        // 幂等：requestId 已存在则直接返回已有记录
        OrderAsyncTask existing = orderAsyncTaskMapper.getByRequestId(requestId);
        if (existing != null) {
            log.info("duplicate requestId, returning existing task: requestId={}", requestId);
            return Result.success(buildStatusVO(existing));
        }

        // 预生成订单号（雪花算法）
        String orderNumber = SnowflakeUtil.nextIdStr();

        // 写入异步任务表
        OrderAsyncTask task = OrderAsyncTask.builder()
                .requestId(requestId)
                .userId(userId)
                .orderNumber(orderNumber)
                .status(OrderAsyncTask.STATUS_PROCESSING)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        try {
            orderAsyncTaskMapper.insert(task);
        } catch (DuplicateKeyException e) {
            // 极端并发下的重复插入，查回已有记录
            existing = orderAsyncTaskMapper.getByRequestId(requestId);
            if (existing != null) {
                return Result.success(buildStatusVO(existing));
            }
            throw e;
        }

        // 发送 MQ 消息（同步发送，保证可靠性）
        OrderSubmitMessage message = OrderSubmitMessage.builder()
                .requestId(requestId)
                .userId(userId)
                .submitDTOJson(JSON.toJSONString(ordersSubmitDTO))
                .build();

        String json = JSON.toJSONString(message);
        Message<String> mqMessage = MessageBuilder.withPayload(json).build();

        try {
            rocketMQTemplate.syncSend(SUBMIT_QUEUE_TOPIC, mqMessage);
        } catch (Exception e) {
            log.error("failed to send order submit message: requestId={}", requestId, e);
            // MQ 发送失败，标记任务失败
            task.setStatus(OrderAsyncTask.STATUS_FAILED);
            task.setErrorMsg("系统繁忙，请稍后重试");
            task.setUpdateTime(LocalDateTime.now());
            orderAsyncTaskMapper.updateFailed(task);
            return Result.error("系统繁忙，请稍后重试");
        }

        return Result.success(buildStatusVO(task));
    }

    /**
     * 查询异步订单处理状态（客户端轮询用）
     *
     * @param requestId 客户端提交时使用的幂等 ID
     * @return 异步任务状态
     */
    @GetMapping("/async-status/{requestId}")
    public Result<OrderAsyncStatusVO> asyncStatus(@PathVariable String requestId) {
        OrderAsyncTask task = orderAsyncTaskMapper.getByRequestId(requestId);
        if (task == null) {
            return Result.error("请求不存在");
        }
        return Result.success(buildStatusVO(task));
    }

    private OrderAsyncStatusVO buildStatusVO(OrderAsyncTask task) {
        String status;
        if (OrderAsyncTask.STATUS_COMPLETED.equals(task.getStatus())) {
            status = "COMPLETED";
        } else if (OrderAsyncTask.STATUS_FAILED.equals(task.getStatus())) {
            status = "FAILED";
        } else {
            status = "PROCESSING";
        }
        return OrderAsyncStatusVO.builder()
                .requestId(task.getRequestId())
                .status(status)
                .orderNumber(task.getOrderNumber())
                .orderId(task.getOrderId())
                .errorMsg(task.getErrorMsg())
                .build();
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @SentinelResource(value = "payOrder", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "payOrder")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @GetMapping("/historyOrders")
    public Result<PageResult> historyOrders(int page, int pageSize, Integer status){
        log.info("历史订单查询");
        PageResult pageResult = orderService.pageQuery(page, pageSize, status);
        return Result.success(pageResult);
    }

    /**
     * 查询订单详细信息
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    public Result<OrderVO> searchOrder(@PathVariable Long id){
        log.info("查询订单详细信息:{}", id);
        OrderVO orderVO = orderService.searchOrder(id);
        return Result.success(orderVO);
    }

    /**
     * 用户取消订单
     *
     * @return
     */
    @PutMapping("/cancel/{id}")
    @SentinelResource(value = "userCancelOrder", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "userCancelOrder")
    public Result cancel(@PathVariable("id") Long id) throws Exception {
        orderService.userCancelById(id);
        return Result.success();
    }

    /**
     * 再来一单
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    public Result repetition(@PathVariable Long id) {
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 催单
     * @return
     */
    @GetMapping("/reminder/{id}")
    public Result reminder(@PathVariable Long id){
        log.info("user催单:{}", id);
        orderService.reminder(id);
        return Result.success();
    }
}
