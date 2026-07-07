-- ============================================================
-- Nacos 2.x → 3.0.x 增量迁移 DDL
-- 在已有 nacos_config 数据库中执行，补充 3.x 新增的表和列
-- 兼容 MySQL 5.7（不支持 ADD COLUMN IF NOT EXISTS）
-- 使用方法: mysql -u root -p nacos_config < nacos-mysql-upgrade.sql
-- ============================================================

USE nacos_config;

-- 1. 新建 config_info_gray 表 (Nacos 2.5.0+)
CREATE TABLE IF NOT EXISTS `config_info_gray` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) NOT NULL COMMENT 'data_id',
  `group_id` varchar(128) NOT NULL COMMENT 'group_id',
  `content` longtext NOT NULL COMMENT 'content',
  `md5` varchar(32) DEFAULT NULL COMMENT 'md5',
  `src_user` text COMMENT 'src_user',
  `src_ip` varchar(100) DEFAULT NULL COMMENT 'src_ip',
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmt_create',
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmt_modified',
  `app_name` varchar(128) DEFAULT NULL COMMENT 'app_name',
  `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
  `gray_name` varchar(128) NOT NULL COMMENT 'gray_name',
  `gray_rule` text NOT NULL COMMENT 'gray_rule',
  `encrypted_data_key` varchar(256) NOT NULL DEFAULT '' COMMENT 'encrypted_data_key',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfogray_datagrouptenantgray` (`data_id`,`group_id`,`tenant_id`,`gray_name`),
  KEY `idx_dataid_gmt_modified` (`data_id`,`gmt_modified`),
  KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='config_info_gray';

-- 2. 为 his_config_info 补充 3.x 新增列 (MySQL 5.7 兼容)
SELECT IF(
  NOT EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='nacos_config' AND TABLE_NAME='his_config_info' AND COLUMN_NAME='publish_type'),
  'ALTER TABLE his_config_info ADD COLUMN `publish_type` varchar(50) DEFAULT \'formal\' COMMENT \'publish type gray or formal\' AFTER `encrypted_data_key`',
  'SELECT 1'
) INTO @sql_publish_type;
PREPARE stmt_publish_type FROM @sql_publish_type;
EXECUTE stmt_publish_type;
DEALLOCATE PREPARE stmt_publish_type;

SELECT IF(
  NOT EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='nacos_config' AND TABLE_NAME='his_config_info' AND COLUMN_NAME='gray_name'),
  'ALTER TABLE his_config_info ADD COLUMN `gray_name` varchar(50) DEFAULT NULL COMMENT \'gray name\' AFTER `publish_type`',
  'SELECT 1'
) INTO @sql_gray_name;
PREPARE stmt_gray_name FROM @sql_gray_name;
EXECUTE stmt_gray_name;
DEALLOCATE PREPARE stmt_gray_name;

SELECT IF(
  NOT EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='nacos_config' AND TABLE_NAME='his_config_info' AND COLUMN_NAME='ext_info'),
  'ALTER TABLE his_config_info ADD COLUMN `ext_info` longtext DEFAULT NULL COMMENT \'ext info\' AFTER `gray_name`',
  'SELECT 1'
) INTO @sql_ext_info;
PREPARE stmt_ext_info FROM @sql_ext_info;
EXECUTE stmt_ext_info;
DEALLOCATE PREPARE stmt_ext_info;

-- 3. 补充 his_config_info 索引
SELECT IF(
  NOT EXISTS(SELECT * FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='nacos_config' AND TABLE_NAME='his_config_info' AND INDEX_NAME='idx_gmt_modified'),
  'ALTER TABLE his_config_info ADD INDEX `idx_gmt_modified` (`gmt_modified`)',
  'SELECT 1'
) INTO @sql_idx;
PREPARE stmt_idx FROM @sql_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;
