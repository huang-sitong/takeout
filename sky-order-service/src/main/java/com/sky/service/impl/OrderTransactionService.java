package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.order.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.OrderBusinessException;
import com.sky.fault.SeataFaultInjector;
import com.sky.feign.CartFeignClient;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.vo.order.OrderSubmitVO;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单核心写事务服务。
 *
 * 将 Seata 全局事务边界收窄到真正的写操作：
 *  - 地址查询、配送范围校验、购物车查询等读/外部调用由 OrderServiceImpl 在事务外完成；
 *  - 订单写入、订单明细写入、购物车清空组成一个全局事务；
 *  - MQ 通知由 OrderServiceImpl 在本类方法成功返回（全局事务已提交）后再发送。
 */
@Service
public class OrderTransactionService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private SeataFaultInjector seataFaultInjector;

    /**
     * 提交订单事务：本地写入订单 + Feign 清空购物车。
     *
     * @param ordersSubmitDTO  下单请求
     * @param addressBook      已在事务外读取的地址
     * @param shoppingCartList 已在事务外读取的购物车
     * @return 订单提交结果
     */
    @GlobalTransactional(name = "submitOrder", timeoutMills = 60000)
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submitOrderInTransaction(OrdersSubmitDTO ordersSubmitDTO,
                                                  AddressBook addressBook,
                                                  List<ShoppingCart> shoppingCartList) {
        Long userId = BaseContext.getCurrentId();

        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);

        orderMapper.insert(orders);
        seataFaultInjector.fire("after-order-insert");

        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        seataFaultInjector.fire("after-order-detail");

        seataFaultInjector.fire("before-cart-clean");
        cartFeignClient.cleanCart();
        seataFaultInjector.fire("after-cart-clean");

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    /**
     * 支付事务：按订单号更新支付状态。
     *
     * 并发防重：先检查状态，再通过带状态条件的 UPDATE 抢占更新；
     * 两个并发请求只有一个能命中 status=1/pay_status=0，另一个返回订单状态错误。
     *
     * @param orderNumber 订单号
     * @return 更新前的订单基础信息（id、number），供事务提交后发送 MQ 使用
     */
    @GlobalTransactional(name = "payment", timeoutMills = 60000)
    @Transactional(rollbackFor = Exception.class)
    public Orders paymentInTransaction(String orderNumber) {
        Orders ordersDB = orderMapper.getByNumber(orderNumber);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!Orders.PENDING_PAYMENT.equals(ordersDB.getStatus())
                || !Orders.UN_PAID.equals(ordersDB.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        int updated = orderMapper.updatePayStatusIfPending(orders);
        if (updated != 1) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        seataFaultInjector.fire("after-payment-update");

        return ordersDB;
    }
}
