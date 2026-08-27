package com.sky.controller.user;

import com.sky.dto.order.OrdersPaymentDTO;
import com.sky.dto.order.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.order.OrderPaymentVO;
import com.sky.vo.order.OrderSubmitVO;
import com.sky.vo.order.OrderVO;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.sky.handler.SentinelBlockHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户提交订单
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @SentinelResource(value = "submitOrder", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "submitOrder")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("提交订单信息:{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
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
     * 查询订单详细信息（精确路径版，避免路径变量触发全量 mapping 遍历）
     * @param id
     * @return
     */
    @GetMapping("/orderDetail")
    public Result<OrderVO> searchOrderByParam(@RequestParam("id") Long id){
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
     * 用户取消订单（精确路径版，避免路径变量触发全量 mapping 遍历）
     *
     * @return
     */
    @PutMapping("/cancel")
    @SentinelResource(value = "userCancelOrder", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "userCancelOrder")
    public Result cancelByParam(@RequestParam("id") Long id) throws Exception {
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
     * 再来一单（精确路径版，避免路径变量触发全量 mapping 遍历）
     * @param id
     * @return
     */
    @PostMapping("/repetition")
    public Result repetitionByParam(@RequestParam("id") Long id) {
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

    /**
     * 催单（精确路径版，避免路径变量触发全量 mapping 遍历）
     * @return
     */
    @GetMapping("/reminder")
    public Result reminderByParam(@RequestParam("id") Long id){
        log.info("user催单:{}", id);
        orderService.reminder(id);
        return Result.success();
    }
}
