SELECT 'TABLE_CHECK' AS section, 'mobile_page_config' AS item, COUNT(*) AS cnt
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mobile_page_config'
UNION ALL
SELECT 'TABLE_CHECK', 'free_query_user', COUNT(*)
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'free_query_user'
UNION ALL
SELECT 'TABLE_CHECK', 'free_query_point_record', COUNT(*)
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'free_query_point_record'
UNION ALL
SELECT 'TABLE_CHECK', 'mark_user_platform_quota', COUNT(*)
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mark_user_platform_quota';

SELECT id, page_code, page_name, status, sort, service_phone, wechat_qr_url, nav_home_url, nav_query_url, nav_batch_url, nav_profile_url, result_back_url, update_time
FROM mobile_page_config
ORDER BY sort, id;

SELECT 'COUNT' AS section, 'free_query_user_active' AS item, COUNT(*) AS cnt
FROM free_query_user
WHERE del_flag = '0'
UNION ALL
SELECT 'COUNT', 'free_query_point_record', COUNT(*)
FROM free_query_point_record
UNION ALL
SELECT 'COUNT', 'mark_user_platform_quota', COUNT(*)
FROM mark_user_platform_quota;

SELECT menu_id, menu_name, parent_id, path, component, perms, status, visible, order_num
FROM sys_menu
WHERE path = 'mobile'
   OR parent_id IN (SELECT menu_id FROM sys_menu WHERE path = 'mobile')
ORDER BY parent_id, order_num, menu_id;

SELECT config_key, config_value
FROM sys_config
WHERE config_key = 'sys.account.captchaEnabled';

SELECT COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'user_api_query_record'
  AND COLUMN_NAME IN ('source_type','device_id','device_source','used_before','used_after','limit_value','error_code','ip_addr')
ORDER BY COLUMN_NAME;

SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'user_api_query_record'
  AND INDEX_NAME IN ('idx_uaqr_source_time','idx_uaqr_device_time','idx_uaqr_error_time','idx_uaqr_task_id','idx_uaqr_ip_time')
GROUP BY INDEX_NAME
ORDER BY INDEX_NAME;

SELECT COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'mark_wallet_log'
  AND COLUMN_NAME IN ('platform_code','platform_name')
ORDER BY COLUMN_NAME;

SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'mark_wallet_log'
  AND INDEX_NAME = 'idx_mark_wallet_log_platform_code'
GROUP BY INDEX_NAME;
