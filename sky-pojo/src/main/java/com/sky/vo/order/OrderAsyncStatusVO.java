package com.sky.vo.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单异步任务状态 VO
 *
 * 客户端提交订单后通过此 VO 轮询处理结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAsyncStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 幂等请求 ID */
    private String requestId;

    /** 处理状态：PROCESSING / COMPLETED / FAILED */
    private String status;

    /** 预生成订单号 */
    private String orderNumber;

    /** 处理完成后的订单 ID */
    private Long orderId;

    /** 失败时的错误信息 */
    private String errorMsg;
}
