package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时的订单
     */
    @Scheduled(cron = "0 */5 * * * ? ")//每5分钟触发一次
    public void processTimeoutOrder(){
        log.info("正在处理超时订单：{}", LocalDateTime.now());
        LocalDateTime limitTime = LocalDateTime.now().plusMinutes(-15);
        List<Orders> list = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT, limitTime);
        if(list != null && !list.isEmpty()){
            for(Orders orders : list){
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，订单取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }


    /**
     * 处理派送时间过长的错误订单
     */
    @Scheduled(cron = "0 0 2 * * ?")//每天2点触发一次
    public void processDeliveryOrder(){
        log.info("正在处理错误订单{}", LocalDateTime.now());
        LocalDateTime limitTime = LocalDateTime.now().plusMinutes(120);
        List<Orders> list = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, limitTime);
        if(list != null && !list.isEmpty()){
            for(Orders orders : list){
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }

}
