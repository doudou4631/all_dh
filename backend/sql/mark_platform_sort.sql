ALTER TABLE mark_platform_mapping
  ADD COLUMN IF NOT EXISTS sort_num int NOT NULL DEFAULT 0 COMMENT '排序';

UPDATE mark_platform_mapping
SET sort_num = CASE source_platform_code
  WHEN 'taidixiong' THEN 1
  WHEN 'tengxun' THEN 2
  WHEN '360fugai' THEN 3
  WHEN 'baidu' THEN 4
  WHEN 'sogou' THEN 5
  WHEN 'yidonggaopin' THEN 6
  WHEN 'dianhuabang' THEN 7
  WHEN 'ltgj' THEN 8
  WHEN 'xiaomi' THEN 9
  ELSE sort_num
END;
