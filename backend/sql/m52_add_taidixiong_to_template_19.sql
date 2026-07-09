SET NAMES utf8mb4;

-- M52: "全部平台2" should include the base Taidixiong platform.
-- Keep quotas unchanged; users still need platform quota before submitting.

START TRANSACTION;

UPDATE mark_platform_template
SET template_info = JSON_ARRAY_INSERT(
        CAST(template_info AS JSON),
        '$[0]',
        JSON_OBJECT('platformCode', 'taidixiong', 'platformName', 'Taidixiong', 'unitPrice', 1)
    ),
    update_by = 'admin',
    update_time = NOW(),
    remark = CASE
      WHEN IFNULL(remark, '') LIKE '%M52-add-taidixiong%' THEN remark
      ELSE CONCAT(IFNULL(remark, ''), ' | M52-add-taidixiong')
    END
WHERE id = 19
  AND JSON_VALID(template_info)
  AND JSON_SEARCH(CAST(template_info AS JSON), 'one', 'taidixiong', NULL, '$[*].platformCode') IS NULL;

COMMIT;
