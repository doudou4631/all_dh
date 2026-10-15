-- 性能增量索引：只增加缺失索引，不覆盖正式库数据。
-- 执行前请先备份 verifynum，重复执行不会报错。

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mark_order_item'
      AND index_name = 'idx_mark_order_item_created_id_order'
);
SET @ddl := IF(
    @idx_exists = 0,
    'ALTER TABLE mark_order_item ADD INDEX idx_mark_order_item_created_id_order (create_time, id, order_id), ALGORITHM=INPLACE, LOCK=NONE',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mark_order_item'
      AND index_name = 'idx_mark_order_item_status_created_id'
);
SET @ddl := IF(
    @idx_exists = 0,
    'ALTER TABLE mark_order_item ADD INDEX idx_mark_order_item_status_created_id (process_status, create_time, id, order_id), ALGORITHM=INPLACE, LOCK=NONE',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
