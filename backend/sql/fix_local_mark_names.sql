SET NAMES utf8mb4;

UPDATE mark_platform_template
SET template_name = '本地测试模板',
    template_info = '[{"platformCode":"mobile_gaopin","platformName":"移动高频","unitPrice":1},{"platformCode":"td_gaopin","platformName":"泰迪高频","unitPrice":1},{"platformCode":"td_second","platformName":"泰迪二次","unitPrice":1},{"platformCode":"qihu_first","platformName":"360首次","unitPrice":1},{"platformCode":"qihu_second","platformName":"360二次","unitPrice":1},{"platformCode":"dianhuabang","platformName":"电话邦","unitPrice":1},{"platformCode":"tencent_mark","platformName":"腾讯","unitPrice":1}]',
    update_by = 'admin',
    update_time = NOW()
WHERE id = 1;

UPDATE mark_user_platform_quota SET platform_name = '移动高频' WHERE user_id = 100001 AND platform_code = 'mobile_gaopin';
UPDATE mark_user_platform_quota SET platform_name = '泰迪高频' WHERE user_id = 100001 AND platform_code = 'td_gaopin';
UPDATE mark_user_platform_quota SET platform_name = '泰迪二次' WHERE user_id = 100001 AND platform_code = 'td_second';
UPDATE mark_user_platform_quota SET platform_name = '360首次' WHERE user_id = 100001 AND platform_code = 'qihu_first';
UPDATE mark_user_platform_quota SET platform_name = '360二次' WHERE user_id = 100001 AND platform_code = 'qihu_second';
UPDATE mark_user_platform_quota SET platform_name = '电话邦' WHERE user_id = 100001 AND platform_code = 'dianhuabang';
UPDATE mark_user_platform_quota SET platform_name = '腾讯' WHERE user_id = 100001 AND platform_code = 'tencent_mark';

UPDATE sys_user SET nick_name = '标记用户' WHERE user_name = 'markuser';
UPDATE sys_user SET nick_name = '标记代理' WHERE user_name = 'markagent';
