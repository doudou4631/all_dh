-- M43: normalize mark platform codes to lowercase and merge duplicate quota rows

SET NAMES utf8mb4;

UPDATE mark_user_platform_quota
SET platform_code = LOWER(TRIM(platform_code))
WHERE BINARY platform_code <> LOWER(TRIM(platform_code));

UPDATE mark_user_platform_price
SET platform_code = LOWER(TRIM(platform_code))
WHERE BINARY platform_code <> LOWER(TRIM(platform_code));

-- Merge duplicate quota rows created by mixed-case platform codes
DELETE q1
FROM mark_user_platform_quota q1
INNER JOIN mark_user_platform_quota q2
    ON q1.user_id = q2.user_id
   AND q1.platform_code = q2.platform_code
   AND q1.id > q2.id;

UPDATE mark_platform_template
SET template_info = REPLACE(
        REPLACE(template_info, '"platformCode":"Baidu"', '"platformCode":"baidu"'),
        '"platformCode":"Xiaomi"', '"platformCode":"xiaomi"'
    )
WHERE template_info LIKE '%"platformCode":"Baidu"%'
   OR template_info LIKE '%"platformCode":"Xiaomi"%';
