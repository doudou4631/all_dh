SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M6：补齐免费查询字典配置（幂等）
-- dict_type: free_query_config
-- =========================================================

INSERT INTO `sys_dict_type` (
  `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`
)
SELECT '免费查询配置', 'free_query_config', '0', 'admin', NOW(), '免费查询每日额度与平台限制配置'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'free_query_config'
);

-- 若类型已存在，确保可用
UPDATE `sys_dict_type`
SET `dict_name` = '免费查询配置',
    `status` = '0',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = '免费查询每日额度与平台限制配置'
WHERE `dict_type` = 'free_query_config';

INSERT INTO `sys_dict_data` (
  `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
  `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`
)
SELECT 1, 'daily_limit', '20', 'free_query_config',
       '', 'default', 'N', '0', 'admin', NOW(), '单IP每日免费查询次数上限'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data`
  WHERE `dict_type` = 'free_query_config' AND `dict_label` = 'daily_limit'
);

INSERT INTO `sys_dict_data` (
  `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
  `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`
)
SELECT 2, 'over_limit_msg', '当前IP今日免费查询次数已达上限，请添加客服微信查询。', 'free_query_config',
       '', 'default', 'N', '0', 'admin', NOW(), '单IP额度用尽提示文案'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data`
  WHERE `dict_type` = 'free_query_config' AND `dict_label` = 'over_limit_msg'
);

INSERT INTO `sys_dict_data` (
  `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
  `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`
)
SELECT 3, 'daily_all_limit', '2000', 'free_query_config',
       '', 'default', 'N', '0', 'admin', NOW(), '平台每日总免费查询次数上限'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data`
  WHERE `dict_type` = 'free_query_config' AND `dict_label` = 'daily_all_limit'
);

INSERT INTO `sys_dict_data` (
  `dict_sort`, `dict_label`, `dict_value`, `dict_type`,
  `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`
)
SELECT 4, 'disabled_platforms', '泰迪熊,联通管家', 'free_query_config',
       '', 'default', 'N', '0', 'admin', NOW(), '免费查询禁用平台名称，英文逗号分隔'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data`
  WHERE `dict_type` = 'free_query_config' AND `dict_label` = 'disabled_platforms'
);

SET FOREIGN_KEY_CHECKS = 1;
