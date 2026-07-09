-- ============================================================
-- 苍穹外卖 — 数据库拆分 5/5: sky_order_db（订单核心）
-- 表: orders, order_detail, undo_log (Seata AT)
-- 所属微服务: sky-order-service (:8086)
-- ============================================================

CREATE DATABASE IF NOT EXISTS `sky_order_db` DEFAULT CHARACTER SET utf8mb3 COLLATE utf8_bin;
USE `sky_order_db`;

-- ============================================================
-- 1. orders（订单）
-- ============================================================
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '订单号',
  `status` int NOT NULL DEFAULT '1' COMMENT '订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 7退款',
  `user_id` bigint NOT NULL COMMENT '下单用户',
  `address_book_id` bigint NOT NULL COMMENT '地址id',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `checkout_time` datetime DEFAULT NULL COMMENT '结账时间',
  `pay_method` int NOT NULL DEFAULT '1' COMMENT '支付方式 1微信,2支付宝',
  `pay_status` tinyint NOT NULL DEFAULT '0' COMMENT '支付状态 0未支付 1已支付 2退款',
  `amount` decimal(10,2) NOT NULL COMMENT '实收金额',
  `remark` varchar(100) COLLATE utf8_bin DEFAULT NULL COMMENT '备注',
  `phone` varchar(11) COLLATE utf8_bin DEFAULT NULL COMMENT '手机号',
  `address` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '地址',
  `user_name` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '用户名称',
  `consignee` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '收货人',
  `cancel_reason` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '订单取消原因',
  `rejection_reason` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '订单拒绝原因',
  `cancel_time` datetime DEFAULT NULL COMMENT '订单取消时间',
  `estimated_delivery_time` datetime DEFAULT NULL COMMENT '预计送达时间',
  `delivery_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '配送状态  1立即送出  0选择具体时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '送达时间',
  `pack_amount` int DEFAULT NULL COMMENT '打包费',
  `tableware_number` int DEFAULT NULL COMMENT '餐具数量',
  `tableware_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '餐具数量状态  1按餐量提供  0选择具体数量',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3 COLLATE=utf8_bin COMMENT='订单表';

-- ============================================================
-- 2. order_detail（订单明细）
-- ============================================================
DROP TABLE IF EXISTS `order_detail`;
CREATE TABLE `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '名字',
  `image` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '图片',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '口味',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3 COLLATE=utf8_bin COMMENT='订单明细表';

-- ============================================================
-- 3. undo_log（Seata AT 模式 — 分布式事务回滚日志）
-- 适用版本: Seata 2.5.0
-- ============================================================
CREATE TABLE IF NOT EXISTS `undo_log` (
  `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `branch_id`     BIGINT(20)   NOT NULL,
  `xid`           VARCHAR(100) NOT NULL,
  `context`       VARCHAR(128) NOT NULL,
  `rollback_info` LONGBLOB     NOT NULL,
  `log_status`    INT(11)      NOT NULL,
  `log_created`   DATETIME     NOT NULL,
  `log_modified`  DATETIME     NOT NULL,
  `ext`           VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
