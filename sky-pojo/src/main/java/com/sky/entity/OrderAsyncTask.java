package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单异步任务表
 *
 * 削峰场景下，订单提交后先写入此表（快速路径），
 * 后台消费者处理完成后更新状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAsyncTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态常量 */
    public static final Integer STATUS_PROCESSING = 0;
    public static final Integer STATUS_COMPLETED = 1;
    public static final Integer STATUS_FAILED = 2;

    /** 主键 */
    private Long id;

    /** 客户端请求幂等 ID（UUID） */
    private String requestId;

    /** 下单用户 ID */
    private Long userId;

    /** 预生成订单号 */
    private String orderNumber;

    /** 状态：0=处理中 1=完成 2=失败 */
    private Integer status;

    /** 处理完成后回填订单 ID */
    private Long orderId;

    /** 失败时的错误信息 */
    private String errorMsg;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
