package com.sky.controller.admin;

import com.sky.dto.order.OrdersCancelDTO;
import com.sky.dto.order.OrdersConfirmDTO;
import com.sky.dto.order.OrdersPageQueryDTO;
import com.sky.dto.order.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.order.OrderStatisticsVO;
import com.sky.vo.order.OrderVO;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.sky.handler.SentinelBlockHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics() {
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 订单详情
     *
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        OrderVO orderVO = orderService.searchOrder(id);
        return Result.success(orderVO);
    }

    /**
     * 订单详情（精确路径版，避免路径变量触发全量 mapping 遍历）
     *
     * @param id
     * @return
     */
    @GetMapping("/details")
    public Result<OrderVO> detailsByParam(@RequestParam("id") Long id) {
        OrderVO orderVO = orderService.searchOrder(id);
        return Result.success(orderVO);
    }

    /**
     * 接单
     *
     * @return
     */
    @PutMapping("/confirm")
    @SentinelResource(value = "confirmOrder", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "confirmOrder")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 拒单
     * @return
     */
    @PutMapping("/rejection")
    @SentinelResource(value = "rejectOrder", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "rejectOrder")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 取消订单
     * @return
     */
    @PutMapping("/cancel")
    @SentinelResource(value = "adminCancelOrder", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "adminCancelOrder")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception {
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 派送订单
     *
     * @return
     */
    @PutMapping("/delivery/{id}")
    public Result delivery(@PathVariable("id") Long id) {
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 派送订单（精确路径版，避免路径变量触发全量 mapping 遍历）
     *
     * @return
     */
    @PutMapping("/delivery")
    public Result deliveryByParam(@RequestParam("id") Long id) {
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     *
     * @return
     */
    @PutMapping("/complete/{id}")
    @SentinelResource(value = "completeOrder", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "completeOrder")
    public Result complete(@PathVariable("id") Long id) {
        orderService.complete(id);
        return Result.success();
    }

    /**
     * 完成订单（精确路径版，避免路径变量触发全量 mapping 遍历）
     *
     * @return
     */
    @PutMapping("/complete")
    @SentinelResource(value = "completeOrder", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "completeOrder")
    public Result completeByParam(@RequestParam("id") Long id) {
        orderService.complete(id);
        return Result.success();
    }
}
