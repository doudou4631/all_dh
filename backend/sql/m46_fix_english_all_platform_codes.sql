SET NAMES utf8mb4;

-- M46: fix English "ȫƽ̨" template invalid platform codes
-- Sougou -> sghmt, LiantongGuanjia -> ltgj
-- Also rename any existing quota/price rows using the wrong codes.

-- 1) Fix template id=4 (Ӣ�İ�ȫƽ̨) and any other templates with the same bad codes
UPDATE mark_platform_template
SET template_info = REPLACE(
        REPLACE(
            REPLACE(
                REPLACE(template_info,
                    '"platformCode":"Sougou"', '"platformCode":"sghmt"'),
                '"platformCode":"sougou"', '"platformCode":"sghmt"'),
            '"platformCode":"LiantongGuanjia"', '"platformCode":"ltgj"'),
        '"platformCode":"liantongguanjia"', '"platformCode":"ltgj"'),
    update_by = 'admin',
    update_time = NOW()
WHERE status = '0'
  AND (
    template_info LIKE '%"platformCode":"Sougou"%'
    OR template_info LIKE '%"platformCode":"sougou"%'
    OR template_info LIKE '%"platformCode":"LiantongGuanjia"%'
    OR template_info LIKE '%"platformCode":"liantongguanjia"%'
  );

-- 2) Rename quota rows: wrong code -> correct code (skip if target already exists)
UPDATE mark_user_platform_quota q
LEFT JOIN mark_user_platform_quota exist
  ON exist.user_id = q.user_id
 AND exist.platform_code = 'sghmt'
SET q.platform_code = 'sghmt',
    q.platform_name = CASE
      WHEN q.platform_name IN ('Sougou', 'sougou') THEN 'Sougou'
      ELSE q.platform_name
    END,
    q.update_by = 'admin',
    q.update_time = NOW()
WHERE LOWER(q.platform_code) = 'sougou'
  AND exist.id IS NULL;

UPDATE mark_user_platform_quota q
LEFT JOIN mark_user_platform_quota exist
  ON exist.user_id = q.user_id
 AND exist.platform_code = 'ltgj'
SET q.platform_code = 'ltgj',
    q.platform_name = CASE
      WHEN q.platform_name IN ('LiantongGuanjia', 'liantongguanjia') THEN 'LiantongGuanjia'
      ELSE q.platform_name
    END,
    q.update_by = 'admin',
    q.update_time = NOW()
WHERE LOWER(q.platform_code) = 'liantongguanjia'
  AND exist.id IS NULL;

-- Drop leftover wrong-code quota rows if correct-code row already exists
DELETE q
FROM mark_user_platform_quota q
INNER JOIN mark_user_platform_quota exist
  ON exist.user_id = q.user_id
 AND exist.platform_code = 'sghmt'
WHERE LOWER(q.platform_code) = 'sougou';

DELETE q
FROM mark_user_platform_quota q
INNER JOIN mark_user_platform_quota exist
  ON exist.user_id = q.user_id
 AND exist.platform_code = 'ltgj'
WHERE LOWER(q.platform_code) = 'liantongguanjia';

-- 3) Rename price rows the same way
UPDATE mark_user_platform_price p
LEFT JOIN mark_user_platform_price exist
  ON exist.user_id = p.user_id
 AND exist.platform_code = 'sghmt'
SET p.platform_code = 'sghmt',
    p.platform_name = CASE
      WHEN p.platform_name IN ('Sougou', 'sougou') THEN 'Sougou'
      ELSE p.platform_name
    END,
    p.update_by = 'admin',
    p.update_time = NOW()
WHERE LOWER(p.platform_code) = 'sougou'
  AND exist.id IS NULL;

UPDATE mark_user_platform_price p
LEFT JOIN mark_user_platform_price exist
  ON exist.user_id = p.user_id
 AND exist.platform_code = 'ltgj'
SET p.platform_code = 'ltgj',
    p.platform_name = CASE
      WHEN p.platform_name IN ('LiantongGuanjia', 'liantongguanjia') THEN 'LiantongGuanjia'
      ELSE p.platform_name
    END,
    p.update_by = 'admin',
    p.update_time = NOW()
WHERE LOWER(p.platform_code) = 'liantongguanjia'
  AND exist.id IS NULL;

DELETE p
FROM mark_user_platform_price p
INNER JOIN mark_user_platform_price exist
  ON exist.user_id = p.user_id
 AND exist.platform_code = 'sghmt'
WHERE LOWER(p.platform_code) = 'sougou';

DELETE p
FROM mark_user_platform_price p
INNER JOIN mark_user_platform_price exist
  ON exist.user_id = p.user_id
 AND exist.platform_code = 'ltgj'
WHERE LOWER(p.platform_code) = 'liantongguanjia';
