package com.sky.mapper;

import com.sky.entity.OrderNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单通知记录 Mapper
 */
@Mapper
public interface OrderNotificationMapper {

    /**
     * 插入通知记录
     * @param notification 通知实体
     */
    void insert(OrderNotification notification);

    /**
     * 根据消息 ID 查询（幂等校验）
     * @param msgId RocketMQ 消息 ID
     * @return 通知记录，不存在返回 null
     */
    OrderNotification getByMsgId(String msgId);
}
