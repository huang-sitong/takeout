package com.sky.mapper;

import com.sky.entity.OrderAsyncTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 订单异步任务 Mapper
 */
@Mapper
public interface OrderAsyncTaskMapper {

    /**
     * 插入异步任务
     */
    void insert(OrderAsyncTask task);

    /**
     * 根据请求 ID 查询（幂等校验）
     */
    @Select("select * from order_async_task where request_id = #{requestId}")
    OrderAsyncTask getByRequestId(String requestId);

    /**
     * 标记任务完成，回填订单 ID
     */
    void updateCompleted(OrderAsyncTask task);

    /**
     * 标记任务失败
     */
    void updateFailed(OrderAsyncTask task);
}
