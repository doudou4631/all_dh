SET NAMES utf8mb4;

-- M50: add td_gaopin to the active all-platform templates.
-- TD_GAOPIN_LOGIC(1).md requires the user template to contain td_gaopin
-- before the template-driven sidebar can expose the Teddy high-frequency page.
-- Do not create quota here; remaining count is still controlled per user.

SET @owner_user_id := 100694622879000173;
SET @english_template_id := 17;
SET @chinese_template_id := 18;

SET @english_template_info := JSON_ARRAY(
    JSON_OBJECT('platformCode', 'taidixiong', 'platformName', 'Taidixiong', 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'td_gaopin', 'platformName', 'TdGaopin', 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'td_second', 'platformName', 'Taidixiong2', 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'qihu_first', 'platformName', '360Fugai', 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'dianhuabang', 'platformName', 'Dianhuabang', 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'tengxun', 'platformName', 'Tengxun', 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'xiaomi', 'platformName', 'Xiaomi', 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'baidu', 'platformName', 'Baidu', 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'yidonggaopin', 'platformName', 'YidongGaopin', 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'sghmt', 'platformName', 'Sougou', 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'ltgj', 'platformName', 'LiantongGuanjia', 'unitPrice', 1)
);

SET @chinese_template_info := JSON_ARRAY(
    JSON_OBJECT('platformCode', 'taidixiong', 'platformName', CONVERT(UNHEX('E6B3B0E8BFAAE7868A') USING utf8mb4), 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'td_gaopin', 'platformName', CONVERT(UNHEX('E6B3B0E8BFAAE7868AE9AB98E9A291') USING utf8mb4), 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'td_second', 'platformName', CONVERT(UNHEX('E6B3B0E8BFAAE7868AE4BA8CE6ACA1') USING utf8mb4), 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'qihu_first', 'platformName', CONVERT(UNHEX('333630E8A686E79B96') USING utf8mb4), 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'dianhuabang', 'platformName', CONVERT(UNHEX('E794B5E8AF9DE982A6') USING utf8mb4), 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'tengxun', 'platformName', CONVERT(UNHEX('E885BEE8AEAF') USING utf8mb4), 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'xiaomi', 'platformName', CONVERT(UNHEX('E5B08FE7B1B3') USING utf8mb4), 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'baidu', 'platformName', CONVERT(UNHEX('E799BEE5BAA6') USING utf8mb4), 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'yidonggaopin', 'platformName', CONVERT(UNHEX('E7A7BBE58AA8E9AB98E9A291') USING utf8mb4), 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'sghmt', 'platformName', CONVERT(UNHEX('E6909CE78B97') USING utf8mb4), 'unitPrice', 1),
    JSON_OBJECT('platformCode', 'ltgj', 'platformName', CONVERT(UNHEX('E88194E9809AE7AEA1E5AEB6') USING utf8mb4), 'unitPrice', 1)
);

START TRANSACTION;

UPDATE mark_platform_template
SET template_info = @english_template_info,
    status = '0',
    is_default = '1',
    owner_user_id = @owner_user_id,
    update_by = 'admin',
    update_time = NOW(),
    remark = 'M50-add-td-gaopin-to-english-all-platform'
WHERE id = @english_template_id;

UPDATE mark_platform_template
SET template_info = @chinese_template_info,
    status = '0',
    is_default = '0',
    owner_user_id = @owner_user_id,
    update_by = 'admin',
    update_time = NOW(),
    remark = 'M50-add-td-gaopin-to-chinese-all-platform'
WHERE id = @chinese_template_id;

COMMIT;
