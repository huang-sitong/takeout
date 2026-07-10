-- ============================================================
-- 苍穹外卖 — 数据库拆分 1/5: sky_admin_db（员工管理）
-- 表: employee
-- 所属微服务: sky-admin-service (:8082)
-- ============================================================

CREATE DATABASE IF NOT EXISTS `sky_admin_db` DEFAULT CHARACTER SET utf8mb3 COLLATE utf8_bin;
USE `sky_admin_db`;

DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) COLLATE utf8_bin NOT NULL COMMENT '姓名',
  `username` varchar(32) COLLATE utf8_bin NOT NULL COMMENT '用户名',
  `password` varchar(64) COLLATE utf8_bin NOT NULL COMMENT '密码',
  `phone` varchar(11) COLLATE utf8_bin NOT NULL COMMENT '手机号',
  `sex` varchar(2) COLLATE utf8_bin NOT NULL COMMENT '性别',
  `id_number` varchar(18) COLLATE utf8_bin NOT NULL COMMENT '身份证号',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用，1:启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 COLLATE=utf8_bin COMMENT='员工信息';

-- 初始管理员账号: admin / 123456 (MD5)
INSERT INTO `employee` VALUES (1,'管理员','admin','e10adc3949ba59abbe56e057f20f883e','13812312312','1','110101199001010047',1,'2022-02-15 15:51:20','2022-02-17 09:16:20',10,1);

-- ============================================================
-- 2. order_notification（订单通知记录 — 审计日志）
-- 纯后端数据存储，不暴露 REST API。
-- 消费 RocketMQ 订单通知消息后写入，供后续审计、统计使用。
-- ============================================================
DROP TABLE IF EXISTS `order_notification`;
CREATE TABLE `order_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` tinyint NOT NULL COMMENT '通知类型：1=支付成功 2=催单 3=新订单 4=取消 5=完成',
  `order_id` bigint NOT NULL COMMENT '关联订单 ID',
  `content` varchar(500) COLLATE utf8_bin NOT NULL COMMENT '通知内容',
  `msg_id` varchar(64) COLLATE utf8_bin NOT NULL COMMENT 'RocketMQ 消息 ID（幂等校验）',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_id` (`msg_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8_bin COMMENT='订单通知记录表（审计日志）';
