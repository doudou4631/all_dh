-- 小米订单明细处理状态扩展：3=处理中(代理已手动提交)
ALTER TABLE mark_order_item
    MODIFY COLUMN process_status char(1) NOT NULL DEFAULT '0'
        COMMENT '处理状态（0待处理 1成功 2失败 3处理中/已手动提交）';
