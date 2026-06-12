SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 回滚 M13：标记业务管理员默认模板与平台API对齐
-- 说明：按本次变更前线上基线回滚
-- =========================================================

SET @old_template_info := '[{"platformCode":"mobile_gaopin","platformName":"高频拦截","unitPrice":1},{"platformCode":"td_gaopin","platformName":"泰迪高频","unitPrice":1},{"platformCode":"dianhuabang","platformName":"电话邦","unitPrice":1},{"platformCode":"tencent_mark","platformName":"腾讯","unitPrice":1}]';

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
SET template_info = @old_template_info,
    update_by = 'oz',
    update_time = NOW()
WHERE id = @target_template_id;

UPDATE user_platform_url_config
SET platform_name = '电话邦',
    url = 'http://api1dhb.tongxinsys.cn:8010/BJQS/ApiCXDHB',
    status = '0',
    update_by = 'oz',
    update_time = NOW()
WHERE id = 3;

UPDATE user_platform_url_config
SET platform_name = '360',
    url = 'http://api1sll.tongxinsys.cn:8010/BJQS/ApiSanLiuL',
    status = '0',
    update_by = 'oz',
    update_time = NOW()
WHERE id = 5;

UPDATE user_platform_url_config
SET platform_name = '360',
    status = '1',
    update_by = 'oz',
    update_time = NOW()
WHERE id = 10;

DELETE FROM user_platform_url_config
WHERE platform_name = '电话邦'
  AND id <> 3
  AND remark = 'restore 电话邦 api';

UPDATE sys_dict_data
SET dict_value = '联通管家,电话邦',
    update_by = 'oz',
    update_time = NOW()
WHERE dict_type = 'free_query_config'
  AND dict_label = 'disabled_platforms';

SET FOREIGN_KEY_CHECKS = 1;
