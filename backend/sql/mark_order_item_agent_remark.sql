-- Execute before deploying the backend that supports agent manual remarks.
-- Compatible with MySQL 8; safe to run again when the column already exists.
SET @agent_remark_ddl = IF(
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'mark_order_item'
          AND column_name = 'agent_remark'
    ),
    'SELECT 1',
    'ALTER TABLE mark_order_item ADD COLUMN agent_remark varchar(500) DEFAULT NULL COMMENT ''代理手动备注'''
);
PREPARE agent_remark_stmt FROM @agent_remark_ddl;
EXECUTE agent_remark_stmt;
DEALLOCATE PREPARE agent_remark_stmt;
