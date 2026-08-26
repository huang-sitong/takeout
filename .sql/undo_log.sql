-- ============================================================
-- Seata AT 模式 undo_log 表（旧单体库 sky_take_out 兼容脚本）
-- 微服务拆分部署请使用 .sql/split/04-sky-cart-db.sql 与 05-sky-order-db.sql 中的 undo_log
-- 适用版本：Seata 1.5.x ~ 2.5.0（undo_log 含 ext 列，结构自 1.5.x 起稳定；本项目部署 2.5.0）
-- 使用方法：
-- 旧单体用法：
--   mysql -u root -p sky_take_out < .sql/undo_log.sql
-- ============================================================

USE `sky_take_out`;

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
