package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.order.GoodsSalesDTO;
import com.sky.dto.order.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 新增订单
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 条件分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 各个状态的订单数量统计
     * @param status
     * @return
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 查询超时的订单
     * @param status
     * @param limitTime
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{limitTime}")
    List<Orders> getByStatusAndOrderTime(Integer status, LocalDateTime limitTime);

    /**
     * 根据日期返回营业额
     * @param map
     * @return
     */
    Double getTurnoverByDates(Map<String, Object> map);

    /**
     * 根据日期和订单转态查询订单数量
     * @param map
     * @return
     */
    Integer countOrdersBetweenTime(Map<String, Object> map);

    /**
     * 获取top10销量的菜品
     * @return
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);
}
