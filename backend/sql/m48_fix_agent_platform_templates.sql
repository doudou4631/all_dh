SET NAMES utf8mb4;

-- M48: restore the platform templates used by the current agent-side navigation.
-- The sidebar is generated from sys_user.rel_mark_template -> mark_platform_template.template_info.
-- Keep the existing template ids so bound users do not need to be rebound.

SET @owner_user_id := 100694622879000173;
SET @english_template_id := 17;
SET @chinese_template_id := 18;

SET @english_template_name := CONVERT(UNHEX('E88BB1E69687E78988E585A8E5B9B3E58FB0') USING utf8mb4);
SET @chinese_template_name := CONVERT(UNHEX('E585A8E5B9B3E58FB0E6A8A1E69DBF') USING utf8mb4);

SET @english_template_info := '[{"platformCode":"taidixiong","platformName":"Taidixiong","unitPrice":1},{"platformCode":"td_second","platformName":"Taidixiong2","unitPrice":1},{"platformCode":"qihu_first","platformName":"360Fugai","unitPrice":1},{"platformCode":"dianhuabang","platformName":"Dianhuabang","unitPrice":1},{"platformCode":"tengxun","platformName":"Tengxun","unitPrice":1},{"platformCode":"xiaomi","platformName":"Xiaomi","unitPrice":1},{"platformCode":"baidu","platformName":"Baidu","unitPrice":1},{"platformCode":"yidonggaopin","platformName":"YidongGaopin","unitPrice":1},{"platformCode":"sghmt","platformName":"Sougou","unitPrice":1},{"platformCode":"ltgj","platformName":"LiantongGuanjia","unitPrice":1}]';
SET @chinese_template_info := CONVERT(UNHEX('5B7B22706C6174666F726D436F6465223A22746169646978696F6E67222C22706C6174666F726D4E616D65223A22E6B3B0E8BFAAE7868A222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A2274645F7365636F6E64222C22706C6174666F726D4E616D65223A22E6B3B0E8BFAAE7868AE4BA8CE6ACA1222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A22716968755F6669727374222C22706C6174666F726D4E616D65223A22333630E8A686E79B96222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A226469616E68756162616E67222C22706C6174666F726D4E616D65223A22E794B5E8AF9DE982A6222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A2274656E6778756E222C22706C6174666F726D4E616D65223A22E885BEE8AEAF222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A227869616F6D69222C22706C6174666F726D4E616D65223A22E5B08FE7B1B3222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A226261696475222C22706C6174666F726D4E616D65223A22E799BEE5BAA6222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A227969646F6E6767616F70696E222C22706C6174666F726D4E616D65223A22E7A7BBE58AA8E9AB98E9A291222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A227367686D74222C22706C6174666F726D4E616D65223A22E6909CE78B97222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A226C74676A222C22706C6174666F726D4E616D65223A22E88194E9809AE7AEA1E5AEB6222C22756E69745072696365223A317D5D') USING utf8mb4);
SET @default_template_info := CONVERT(UNHEX('5B7B22706C6174666F726D436F6465223A22746169646978696F6E67222C22706C6174666F726D4E616D65223A22E6B3B0E8BFAAE7868A222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A2274656E6778756E222C22706C6174666F726D4E616D65223A22E885BEE8AEAF222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A2273616E6C69756C696E67222C22706C6174666F726D4E616D65223A22333630222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A226261696475222C22706C6174666F726D4E616D65223A22E799BEE5BAA6222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A227367686D74222C22706C6174666F726D4E616D65223A22E6909CE78B97222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A227969646F6E6767616F70696E222C22706C6174666F726D4E616D65223A22E7A7BBE58AA8E9AB98E9A291222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A227869616F6D69222C22706C6174666F726D4E616D65223A22E5B08FE7B1B3E6898BE69CBA222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A226C74676A222C22706C6174666F726D4E616D65223A22E88194E9809AE7AEA1E5AEB6222C22756E69745072696365223A317D2C7B22706C6174666F726D436F6465223A226469616E68756162616E67222C22706C6174666F726D4E616D65223A22E794B5E8AF9DE982A6222C22756E69745072696365223A317D5D') USING utf8mb4);

START TRANSACTION;

UPDATE mark_platform_template
SET is_default = '0',
    update_by = 'admin',
    update_time = NOW()
WHERE owner_user_id = @owner_user_id
  AND id <> @english_template_id
  AND is_default = '1';

UPDATE mark_platform_template
SET template_name = @english_template_name,
    template_info = @english_template_info,
    status = '0',
    is_default = '1',
    update_by = 'admin',
    update_time = NOW(),
    remark = 'M48-agent-english-all-platform'
WHERE id = @english_template_id
  AND owner_user_id = @owner_user_id;

UPDATE mark_platform_template
SET template_name = @chinese_template_name,
    template_info = @chinese_template_info,
    status = '0',
    is_default = '0',
    update_by = 'admin',
    update_time = NOW(),
    remark = 'M48-agent-chinese-all-platform'
WHERE id = @chinese_template_id
  AND owner_user_id = @owner_user_id;

-- The old global default template was missing 360/LTGJ and had a typo in Teddy.
UPDATE mark_platform_template
SET template_info = @default_template_info,
    update_by = 'admin',
    update_time = NOW(),
    remark = CONCAT(IFNULL(remark, ''), ' | M48-default-9-platform')
WHERE id = 4
  AND status = '0';

-- Saved price/quota names override template names, so align current bound users too.
UPDATE mark_user_platform_price p
JOIN sys_user u ON u.user_id = p.user_id
SET p.platform_name = CASE LOWER(p.platform_code)
        WHEN 'taidixiong' THEN 'Taidixiong'
        WHEN 'td_second' THEN 'Taidixiong2'
        WHEN 'qihu_first' THEN '360Fugai'
        WHEN 'dianhuabang' THEN 'Dianhuabang'
        WHEN 'tengxun' THEN 'Tengxun'
        WHEN 'xiaomi' THEN 'Xiaomi'
        WHEN 'baidu' THEN 'Baidu'
        WHEN 'yidonggaopin' THEN 'YidongGaopin'
        WHEN 'sghmt' THEN 'Sougou'
        WHEN 'ltgj' THEN 'LiantongGuanjia'
        ELSE p.platform_name
    END,
    p.update_by = 'admin',
    p.update_time = NOW()
WHERE u.rel_mark_template = @english_template_id
  AND LOWER(p.platform_code) IN ('taidixiong','td_second','qihu_first','dianhuabang','tengxun','xiaomi','baidu','yidonggaopin','sghmt','ltgj');

UPDATE mark_user_platform_quota q
JOIN sys_user u ON u.user_id = q.user_id
SET q.platform_name = CASE LOWER(q.platform_code)
        WHEN 'taidixiong' THEN 'Taidixiong'
        WHEN 'td_second' THEN 'Taidixiong2'
        WHEN 'qihu_first' THEN '360Fugai'
        WHEN 'dianhuabang' THEN 'Dianhuabang'
        WHEN 'tengxun' THEN 'Tengxun'
        WHEN 'xiaomi' THEN 'Xiaomi'
        WHEN 'baidu' THEN 'Baidu'
        WHEN 'yidonggaopin' THEN 'YidongGaopin'
        WHEN 'sghmt' THEN 'Sougou'
        WHEN 'ltgj' THEN 'LiantongGuanjia'
        ELSE q.platform_name
    END,
    q.update_by = 'admin',
    q.update_time = NOW()
WHERE u.rel_mark_template = @english_template_id
  AND LOWER(q.platform_code) IN ('taidixiong','td_second','qihu_first','dianhuabang','tengxun','xiaomi','baidu','yidonggaopin','sghmt','ltgj');

UPDATE mark_user_platform_price p
JOIN sys_user u ON u.user_id = p.user_id
SET p.platform_name = CASE LOWER(p.platform_code)
        WHEN 'taidixiong' THEN CONVERT(UNHEX('E6B3B0E8BFAAE7868A') USING utf8mb4)
        WHEN 'tengxun' THEN CONVERT(UNHEX('E885BEE8AEAF') USING utf8mb4)
        WHEN 'sanliuling' THEN '360'
        WHEN 'baidu' THEN CONVERT(UNHEX('E799BEE5BAA6') USING utf8mb4)
        WHEN 'sghmt' THEN CONVERT(UNHEX('E6909CE78B97') USING utf8mb4)
        WHEN 'yidonggaopin' THEN CONVERT(UNHEX('E7A7BBE58AA8E9AB98E9A291') USING utf8mb4)
        WHEN 'xiaomi' THEN CONVERT(UNHEX('E5B08FE7B1B3E6898BE69CBA') USING utf8mb4)
        WHEN 'ltgj' THEN CONVERT(UNHEX('E88194E9809AE7AEA1E5AEB6') USING utf8mb4)
        WHEN 'dianhuabang' THEN CONVERT(UNHEX('E794B5E8AF9DE982A6') USING utf8mb4)
        ELSE p.platform_name
    END,
    p.update_by = 'admin',
    p.update_time = NOW()
WHERE u.rel_mark_template = 4
  AND LOWER(p.platform_code) IN ('taidixiong','tengxun','sanliuling','baidu','sghmt','yidonggaopin','xiaomi','ltgj','dianhuabang');

UPDATE mark_user_platform_quota q
JOIN sys_user u ON u.user_id = q.user_id
SET q.platform_name = CASE LOWER(q.platform_code)
        WHEN 'taidixiong' THEN CONVERT(UNHEX('E6B3B0E8BFAAE7868A') USING utf8mb4)
        WHEN 'tengxun' THEN CONVERT(UNHEX('E885BEE8AEAF') USING utf8mb4)
        WHEN 'sanliuling' THEN '360'
        WHEN 'baidu' THEN CONVERT(UNHEX('E799BEE5BAA6') USING utf8mb4)
        WHEN 'sghmt' THEN CONVERT(UNHEX('E6909CE78B97') USING utf8mb4)
        WHEN 'yidonggaopin' THEN CONVERT(UNHEX('E7A7BBE58AA8E9AB98E9A291') USING utf8mb4)
        WHEN 'xiaomi' THEN CONVERT(UNHEX('E5B08FE7B1B3E6898BE69CBA') USING utf8mb4)
        WHEN 'ltgj' THEN CONVERT(UNHEX('E88194E9809AE7AEA1E5AEB6') USING utf8mb4)
        WHEN 'dianhuabang' THEN CONVERT(UNHEX('E794B5E8AF9DE982A6') USING utf8mb4)
        ELSE q.platform_name
    END,
    q.update_by = 'admin',
    q.update_time = NOW()
WHERE u.rel_mark_template = 4
  AND LOWER(q.platform_code) IN ('taidixiong','tengxun','sanliuling','baidu','sghmt','yidonggaopin','xiaomi','ltgj','dianhuabang');

COMMIT;
