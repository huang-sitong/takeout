package com.sky.handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.sky.dto.order.OrdersCancelDTO;
import com.sky.dto.order.OrdersConfirmDTO;
import com.sky.dto.order.OrdersPaymentDTO;
import com.sky.dto.order.OrdersRejectionDTO;
import com.sky.dto.order.OrdersSubmitDTO;
import com.sky.result.Result;

/**
 * Sentinel @SentinelResource 统一阻塞处理类。
 * 方法签名必须与原 controller 方法逐参数匹配并追加 BlockException 参数，
 * 返回类型与原方法一致。限流/熔断统一返回 Result{code:0, msg:...}。
 * 规则由 Nacos 数据源 (sky-order-service-flow-rules.json / sky-order-service-degrade-rules.json) 持久化下发。
 */
public class SentinelBlockHandler {

    private static Result fail(BlockException ex) {
        String msg = "系统繁忙，请稍后再试";
        if (ex instanceof FlowException) {
            msg = "请求过于频繁，请稍后再试";
        } else if (ex instanceof DegradeException) {
            msg = "当前服务不可用，请稍后再试";
        }
        return Result.error(msg);
    }

    public static Result<com.sky.vo.order.OrderSubmitVO> submitOrder(OrdersSubmitDTO dto, BlockException ex) {
        return fail(ex);
    }

    public static Result<com.sky.vo.order.OrderPaymentVO> payOrder(OrdersPaymentDTO dto, BlockException ex) {
        return fail(ex);
    }

    public static Result userCancelOrder(Long id, BlockException ex) {
        return fail(ex);
    }

    public static Result confirmOrder(OrdersConfirmDTO dto, BlockException ex) {
        return fail(ex);
    }

    public static Result rejectOrder(OrdersRejectionDTO dto, BlockException ex) {
        return fail(ex);
    }

    public static Result adminCancelOrder(OrdersCancelDTO dto, BlockException ex) {
        return fail(ex);
    }

    public static Result completeOrder(Long id, BlockException ex) {
        return fail(ex);
    }
}