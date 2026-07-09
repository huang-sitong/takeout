-- ============================================================
-- 苍穹外卖 — 数据库拆分 2/5: sky_user_db（用户 + 地址）
-- 表: user, address_book
-- 所属微服务: sky-user-service (:8083)
-- ============================================================

CREATE DATABASE IF NOT EXISTS `sky_user_db` DEFAULT CHARACTER SET utf8mb3 COLLATE utf8_bin;
USE `sky_user_db`;

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid` varchar(45) COLLATE utf8_bin DEFAULT NULL COMMENT '微信用户唯一标识',
  `name` varchar(32) COLLATE utf8_bin DEFAULT NULL COMMENT '姓名',
  `phone` varchar(11) COLLATE utf8_bin DEFAULT NULL COMMENT '手机号',
  `sex` varchar(2) COLLATE utf8_bin DEFAULT NULL COMMENT '性别',
  `id_number` varchar(18) COLLATE utf8_bin DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(500) COLLATE utf8_bin DEFAULT NULL COMMENT '头像',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3 COLLATE=utf8_bin COMMENT='用户信息';

DROP TABLE IF EXISTS `address_book`;
CREATE TABLE `address_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `consignee` varchar(50) COLLATE utf8_bin DEFAULT NULL COMMENT '收货人',
  `sex` varchar(2) COLLATE utf8_bin DEFAULT NULL COMMENT '性别',
  `phone` varchar(11) COLLATE utf8_bin NOT NULL COMMENT '手机号',
  `province_code` varchar(12) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '省级区划编号',
  `province_name` varchar(32) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '省级名称',
  `city_code` varchar(12) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '市级区划编号',
  `city_name` varchar(32) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '市级名称',
  `district_code` varchar(12) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '区级区划编号',
  `district_name` varchar(32) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '区级名称',
  `detail` varchar(200) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '详细地址',
  `label` varchar(100) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '标签',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '默认 0 否 1是',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 COLLATE=utf8_bin COMMENT='地址簿';
