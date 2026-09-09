ALTER TABLE `free_query_page_config`
  ADD COLUMN IF NOT EXISTS `show_online_service` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否显示在线客服按钮' AFTER `qr_code`;
