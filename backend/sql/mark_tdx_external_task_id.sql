-- Run once before enabling the external TDX line2 adapter.
-- Existing rows remain valid because MySQL permits multiple NULLs in a unique key.
ALTER TABLE mark_order_item
    ADD COLUMN external_task_id varchar(160) DEFAULT NULL COMMENT '外部线路2任务号（幂等）';

ALTER TABLE mark_order_item
    ADD UNIQUE KEY uk_mark_order_item_external_task_id (external_task_id);
