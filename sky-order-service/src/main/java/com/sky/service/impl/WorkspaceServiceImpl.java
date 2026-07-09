package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.feign.MenuFeignClient;
import com.sky.feign.UserFeignClient;
import com.sky.mapper.OrderMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.order.BusinessDataVO;
import com.sky.vo.menu.DishOverViewVO;
import com.sky.vo.order.OrderOverViewVO;
import com.sky.vo.menu.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private MenuFeignClient menuFeignClient;

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */

        Map map = new HashMap();
        map.put("begin",begin);
        map.put("end",end);

        //查询总订单数
        Integer totalOrderCount = orderMapper.countOrdersBetweenTime(map);

        map.put("status", Orders.COMPLETED);
        //营业额
        Double turnover = orderMapper.getTurnoverByDates(map);
        turnover = turnover == null? 0.0 : turnover;

        //有效订单数
        Integer validOrderCount = orderMapper.countOrdersBetweenTime(map);

        Double unitPrice = 0.0;

        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0 && validOrderCount != 0){
            //订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            //平均客单价
            unitPrice = turnover / validOrderCount;
        }

        //新增用户数（通过 Feign 调用 user-service）
        Integer newUsers = userFeignClient.countUserByDates(begin, end).getData();

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }


    /**
     * 查询订单管理数据
     *
     * @return
     */
    public OrderOverViewVO getOrderOverView() {
        Map map = new HashMap();
        map.put("begin", LocalDateTime.now().with(LocalTime.MIN));
        map.put("status", Orders.TO_BE_CONFIRMED);

        //待接单
        Integer waitingOrders = orderMapper.countOrdersBetweenTime(map);

        //待派送
        map.put("status", Orders.CONFIRMED);
        Integer deliveredOrders = orderMapper.countOrdersBetweenTime(map);

        //已完成
        map.put("status", Orders.COMPLETED);
        Integer completedOrders = orderMapper.countOrdersBetweenTime(map);

        //已取消
        map.put("status", Orders.CANCELLED);
        Integer cancelledOrders = orderMapper.countOrdersBetweenTime(map);

        //全部订单
        map.put("status", null);
        Integer allOrders = orderMapper.countOrdersBetweenTime(map);

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    public DishOverViewVO getDishOverView() {
        // 通过 Feign 调用 menu-service 统计菜品状态
        Integer sold = menuFeignClient.countDishByStatus(StatusConstant.ENABLE).getData();
        Integer discontinued = menuFeignClient.countDishByStatus(StatusConstant.DISABLE).getData();

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    public SetmealOverViewVO getSetmealOverView() {
        // 通过 Feign 调用 menu-service 统计套餐状态
        Integer sold = menuFeignClient.countSetmealByStatus(StatusConstant.ENABLE).getData();
        Integer discontinued = menuFeignClient.countSetmealByStatus(StatusConstant.DISABLE).getData();

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }
}
