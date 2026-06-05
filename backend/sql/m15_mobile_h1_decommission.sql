SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M15：mobile-h1 兼容壳最终下线
-- 1) 停用 mobile_page_config 中 mobile-h1 页面配置
-- =========================================================

UPDATE `mobile_page_config`
SET `status` = '1',
    `update_by` = 'admin',
    `update_time` = NOW(),
    `remark` = 'M15-下线mobile-h1兼容壳，停用mobile-h1页面配置'
WHERE `page_code` = 'mobile-h1'
  AND `del_flag` = '0';

SET FOREIGN_KEY_CHECKS = 1;
