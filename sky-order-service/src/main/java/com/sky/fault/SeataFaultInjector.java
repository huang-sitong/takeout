package com.sky.fault;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Seata 回滚测试故障注入器。
 *
 * 通过配置 test.seata.fault-point 指定注入点：
 *  - after-order-insert   订单主表插入后抛异常
 *  - after-order-detail   订单明细插入后抛异常
 *  - before-cart-clean    清空购物车前抛异常
 *  - after-cart-clean     清空购物车后抛异常
 *  - after-payment-update 支付状态更新后抛异常
 *
 * 默认 none，生产与普通测试不生效。
 */
@Component
public class SeataFaultInjector {

    @Value("${test.seata.fault-point:none}")
    private String faultPoint;

    /**
     * 在指定事务关键点触发故障。
     *
     * @param point 当前执行到的注入点名称
     */
    public void fire(String point) {
        if (Objects.equals(point, faultPoint)) {
            throw new IllegalStateException("Injected Seata test fault at: " + point);
        }
    }
}
