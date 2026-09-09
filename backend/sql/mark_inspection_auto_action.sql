ALTER TABLE mark_inspection_policy
  ADD COLUMN IF NOT EXISTS auto_action varchar(10) NOT NULL DEFAULT 'SUCCESS' AFTER batch_limit;

UPDATE mark_inspection_policy
SET auto_action = 'SUCCESS'
WHERE auto_action IS NULL OR auto_action NOT IN ('SUCCESS', 'FAILED');
