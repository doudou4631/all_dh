SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M13：标记业务管理员默认模板与平台API对齐
-- 1) 默认模板更新为9个平台（含联通管家/电话邦）
-- 2) 360仅保留 id=3 启用，并固定 URL
-- 3) 补回/启用电话邦 API 记录
-- 4) free_query_config.disabled_platforms 去掉联通管家/联通安全管家/电话邦
-- =========================================================

SET @new_template_info := '[{"platformCode":"taidixiong","platformName":"泰迪熊","unitPrice":1},{"platformCode":"tengxun","platformName":"腾讯","unitPrice":1},{"platformCode":"sanliuling","platformName":"360","unitPrice":1},{"platformCode":"baidu","platformName":"百度","unitPrice":1},{"platformCode":"sghmt","platformName":"搜狗","unitPrice":1},{"platformCode":"yidonggaopin","platformName":"移动高频","unitPrice":1},{"platformCode":"xiaomi","platformName":"小米手机","unitPrice":1},{"platformCode":"ltgj","platformName":"联通管家","unitPrice":1},{"platformCode":"dianhuabang","platformName":"电话邦","unitPrice":1}]';

SET @target_template_id := (
  SELECT t.id
  FROM mark_platform_template t
  JOIN sys_user_role ur ON ur.user_id = t.owner_user_id
  JOIN sys_role r ON r.role_id = ur.role_id
  WHERE r.role_key = 'mark_admin'
    AND t.is_default = '1'
    AND t.status = '0'
  ORDER BY t.id
  LIMIT 1
);
SET @target_template_id := COALESCE(
  @target_template_id,
  (SELECT id FROM mark_platform_template WHERE is_default = '1' AND status = '0' ORDER BY id LIMIT 1)
);

UPDATE mark_platform_template
SET template_info = @new_template_info,
    update_by = 'oz',
    update_time = NOW()
WHERE id = @target_template_id;

UPDATE user_platform_url_config
SET platform_name = '360',
    url = 'http://api1sll.tongxinsys.cn:8010/BJQS/ApiSanLiuL',
    status = '0',
    update_by = 'oz',
    update_time = NOW()
WHERE id = 3;

UPDATE user_platform_url_config
SET status = '1',
    update_by = 'oz',
    update_time = NOW()
WHERE platform_name = '360'
  AND id <> 3;

SET @dhb_url := 'http://api1dhb.tongxinsys.cn:8010/BJQS/ApiCXDHB';
SET @dhb_count := (SELECT COUNT(*) FROM user_platform_url_config WHERE platform_name = '电话邦');

UPDATE user_platform_url_config
SET status = '0',
    url = CASE WHEN url IS NULL OR url = '' THEN @dhb_url ELSE url END,
    update_by = 'oz',
    update_time = NOW()
WHERE platform_name = '电话邦';

INSERT INTO user_platform_url_config
(platform_name, url, request_interval_ms, timeout_ms, retry_count, pre_action_type, concurrency_limit, sort, status, create_by, create_time, update_by, update_time, remark)
SELECT '电话邦', @dhb_url, 1000, 5000, 3, 0, 1, 8, '0', 'oz', NOW(), 'oz', NOW(), 'restore 电话邦 api'
WHERE @dhb_count = 0;

UPDATE user_platform_url_config
SET status = '0',
    update_by = 'oz',
    update_time = NOW()
WHERE platform_name IN ('联通管家', '联通安全管家');

INSERT INTO sys_dict_data
(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT 99, 'disabled_platforms', '', 'free_query_config', '', 'default', 'N', '0', 'oz', NOW(), 'oz', NOW(), 'free query disabled platforms'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data
  WHERE dict_type = 'free_query_config'
    AND dict_label = 'disabled_platforms'
);

UPDATE sys_dict_data
SET dict_value = TRIM(BOTH ',' FROM REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(CONCAT(',', REPLACE(IFNULL(dict_value, ''), '，', ','), ','), ',联通管家,', ','), ',联通安全管家,', ','), ',电话邦,', ','), ',,', ','), ',,', ',')),
    update_by = 'oz',
    update_time = NOW()
WHERE dict_type = 'free_query_config'
  AND dict_label = 'disabled_platforms';

SET FOREIGN_KEY_CHECKS = 1;
