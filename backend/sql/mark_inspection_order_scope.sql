-- Run this incremental migration only; mark_auto_inspection.sql resets existing policy switches.
-- NULL distinguishes existing rows on first migration, so repeated execution preserves user settings.
ALTER TABLE mark_inspection_policy
  ADD COLUMN IF NOT EXISTS order_scope varchar(16) DEFAULT NULL COMMENT 'PENDING/PROCESSING/BOTH' AFTER auto_action,
  ADD COLUMN IF NOT EXISTS auto_takeover tinyint(1) DEFAULT NULL COMMENT 'Automatically handle pending orders' AFTER order_scope;

UPDATE mark_inspection_policy
SET order_scope = COALESCE(order_scope,
      CASE WHEN LOWER(platform_code) IN ('wuma_taidixiong','wuma_tengxun','wuma_360','xiaomi') THEN 'BOTH' ELSE 'PENDING' END),
    auto_takeover = COALESCE(auto_takeover,
      CASE WHEN LOWER(platform_code) IN ('wuma_taidixiong','wuma_tengxun','wuma_360') THEN 1 ELSE 0 END)
WHERE order_scope IS NULL OR auto_takeover IS NULL;

ALTER TABLE mark_inspection_run_log
  ADD COLUMN IF NOT EXISTS checked_count int NOT NULL DEFAULT 0 AFTER scanned_count,
  ADD COLUMN IF NOT EXISTS marked_count int NOT NULL DEFAULT 0 AFTER checked_count;
