-- ============================================================
-- 苍穹外卖 — 数据库拆分：一键执行全部 5 个库
-- 使用方法:
--   mysql -u root -p < .sql/split/00-run-all.sql
-- 或逐个执行:
--   mysql -u root -p < .sql/split/01-sky-admin-db.sql
--   mysql -u root -p < .sql/split/02-sky-user-db.sql
--   mysql -u root -p < .sql/split/03-sky-menu-db.sql
--   mysql -u root -p < .sql/split/04-sky-cart-db.sql
--   mysql -u root -p < .sql/split/05-sky-order-db.sql
-- ============================================================

SOURCE 01-sky-admin-db.sql;
SOURCE 02-sky-user-db.sql;
SOURCE 03-sky-menu-db.sql;
SOURCE 04-sky-cart-db.sql;
SOURCE 05-sky-order-db.sql;

-- 验证: 查看所有新数据库
SHOW DATABASES LIKE 'sky_%_db';
