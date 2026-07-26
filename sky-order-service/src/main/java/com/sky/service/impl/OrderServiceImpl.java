package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.order.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.feign.CartFeignClient;
import com.sky.feign.UserFeignClient;
import com.sky.mapper.*;
import com.sky.mq.RocketMQProducerService;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.SnowflakeUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.order.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService{

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private RocketMQProducerService rocketMQProducerService;

    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;

    /**
     * 提交订单
     * @param ordersSubmitDTO
     * @return
     */
    @GlobalTransactional(name = "submitOrder", timeoutMills = 60000)
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO){
        // 通过 Feign 远程获取地址信息
        Result<AddressBook> addressResult = userFeignClient.getAddressById(ordersSubmitDTO.getAddressBookId());
        AddressBook addressBook = addressResult.getData();
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //检查是否在配送范围内
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() +  addressBook.getDetail());

        // 通过 Feign 远程获取该用户的购物车
        Long userId = BaseContext.getCurrentId();
        Result<List<ShoppingCart>> cartResult = cartFeignClient.listByUserId();
        List<ShoppingCart> shoppingCartList = cartResult.getData();
        if(shoppingCartList == null || shoppingCartList.isEmpty()){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单录入数据传输对象的数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        //补充订单的其他信息
        orders.setOrderTime(LocalDateTime.now());
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        //向数据库中插入数据
        orderMapper.insert(orders);

        //批量插入订单细节信息
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for(ShoppingCart cart : shoppingCartList){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        // 通过 Feign 远程清空购物车
        cartFeignClient.cleanCart();

        // 发送新订单通知到管理端（异步，不阻断主流程）
        rocketMQProducerService.sendNewOrderNotification(orders.getId(), orders.getNumber(), orders.getAmount().toString());

        //构造返回的数据
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }

    /**
     * 异步处理订单创建（削峰消费者调用）
     *
     * 迁移自 submitOrder 的业务逻辑，用于后台消费者按速率消费。
     * 与 submitOrder 不同的是：
     * - 显式传入 userId（消费者线程无 HTTP 上下文）
     * - orderNumber 使用雪花算法生成（高并发防重）
     * - 不返回 VO（处理结果通过 update order_async_task 记录）
     *
     * @param requestId 客户端幂等请求 ID
     * @param userId    下单用户 ID
     * @param submitDTO 订单提交 DTO
     */
    @GlobalTransactional(name = "processOrderCreation", timeoutMills = 60000)
    @Transactional
    public void processOrderCreation(String requestId, Long userId, OrdersSubmitDTO submitDTO) {
        // 通过 Feign 远程获取地址信息
        Result<AddressBook> addressResult = userFeignClient.getAddressById(submitDTO.getAddressBookId());
        AddressBook addressBook = addressResult.getData();
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 检查是否在配送范围内
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

        // 通过 Feign 远程获取该用户的购物车
        Result<List<ShoppingCart>> cartResult = cartFeignClient.listByUserId();
        List<ShoppingCart> shoppingCartList = cartResult.getData();
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 向订单录入数据传输对象的数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(submitDTO, orders);
        // 补充订单的其他信息
        orders.setOrderTime(LocalDateTime.now());
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setNumber(SnowflakeUtil.nextIdStr());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        // 向数据库中插入数据
        orderMapper.insert(orders);

        // 批量插入订单细节信息
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        // 通过 Feign 远程清空购物车
        cartFeignClient.cleanCart();

        // 发送新订单通知到管理端（异步，不阻断主流程）
        rocketMQProducerService.sendNewOrderNotification(orders.getId(), orders.getNumber(), orders.getAmount().toString());
    }

    /**
     * 订单支付
     */
    @GlobalTransactional(name = "payment", timeoutMills = 60000)
    @Transactional
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        //绕过微信支付
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        paySuccess(ordersPaymentDTO.getOrderNumber());
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     */
    public void paySuccess(String outTradeNo) {
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //通过消息队列向admin端发送支付成功通知
        rocketMQProducerService.sendOrderNotification(ordersDB.getId(), 1, "订单号：" + outTradeNo);
    }

    /**
     * 历史订单查询
     */
    public PageResult pageQuery(int pageNum, int pageSize, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        for(Orders orders : page){
            Long orderId = orders.getId();
            List<OrderDetail> details = orderDetailMapper.getByOrderId(orderId);
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders,orderVO);
            orderVO.setOrderDetailList(details);
            list.add(orderVO);
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详细信息
     */
    public OrderVO searchOrder(Long id) {
        OrderVO orderVO = new OrderVO();
        Orders orders = orderMapper.getById(id);
        BeanUtils.copyProperties(orders,orderVO);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 取消订单
     */
    public void userCancelById(Long id) {
        Orders order = orderMapper.getById(id);
        if(order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(order.getStatus() > Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders newOrder = new Orders();
        newOrder.setId(order.getId());
        newOrder.setStatus(Orders.CANCELLED);
        newOrder.setCancelReason("用户取消");
        newOrder.setCancelTime(LocalDateTime.now());
        orderMapper.update(newOrder);

        // 发送订单取消通知到管理端
        rocketMQProducerService.sendOrderCancelNotification(order.getId(), "用户取消", "用户");
    }

    /**
     * 再来一单
     */
    public void repetition(Long id) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        // 将订单详情转为购物车条目（购物车表在 cart-service，但重复下单逻辑保留在 order-service 本地）
        // 注意：此方法直接操作 order_detail 表，不涉及跨服务调用
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        for(OrderDetail orderDetail : orderDetailList){
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,shoppingCart);
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartList.add(shoppingCart);
        }
        // 通过 Feign 批量添加购物车条目
        for(ShoppingCart cart : shoppingCartList){
            cartFeignClient.addCartItem(cart);
        }
    }

    /**
     * admin端订单条件分页查询
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        for(Orders orders : page){
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders,orderVO);
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
            String orderDishes = detailList2String(orderDetailList);
            orderVO.setOrderDishes(orderDishes);
            list.add(orderVO);
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 各个状态的订单数量统计
     */
    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders order = orderMapper.getById(ordersRejectionDTO.getId());
        if(order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(!Objects.equals(order.getStatus(), Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders newOrder = new Orders();
        newOrder.setId(order.getId());
        newOrder.setStatus(Orders.CANCELLED);
        newOrder.setCancelReason(ordersRejectionDTO.getRejectionReason());
        newOrder.setCancelTime(LocalDateTime.now());
        orderMapper.update(newOrder);

        // 发送订单取消通知（拒单）
        rocketMQProducerService.sendOrderCancelNotification(order.getId(), ordersRejectionDTO.getRejectionReason(), "商家");
    }

    /**
     * 取消订单（admin端）
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Orders order = orderMapper.getById(ordersCancelDTO.getId());

        Orders newOrder = new Orders();
        newOrder.setId(order.getId());
        newOrder.setStatus(Orders.CANCELLED);
        newOrder.setCancelReason(ordersCancelDTO.getCancelReason());
        newOrder.setCancelTime(LocalDateTime.now());
        orderMapper.update(newOrder);

        // 发送订单取消通知（管理员取消）
        rocketMQProducerService.sendOrderCancelNotification(order.getId(), ordersCancelDTO.getCancelReason(), "管理员");
    }

    /**
     * 派送订单
     */
    public void delivery(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null || !orders.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders newOrders = new Orders();
        newOrders.setId(orders.getId());
        newOrders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(newOrders);
    }

    /**
     * 完成订单
     */
    public void complete(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders newOrders = new Orders();
        newOrders.setId(orders.getId());
        newOrders.setStatus(Orders.COMPLETED);
        newOrders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(newOrders);

        // 发送订单完成通知
        rocketMQProducerService.sendOrderCompleteNotification(orders.getId(), orders.getNumber());
    }

    /**
     * 催单
     */
    public void reminder(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        rocketMQProducerService.sendOrderNotification(orders.getId(), 2, orders.getNumber());
    }

    private String detailList2String(List<OrderDetail> orderDetailList){
        String res = "";
        for(OrderDetail orderDetail : orderDetailList){
            res += orderDetail.getName() + "*" + orderDetail.getNumber() + ";";
        }
        return res;
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            throw new OrderBusinessException("超出配送范围");
        }
    }
}
