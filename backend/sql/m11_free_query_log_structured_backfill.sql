SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- M11 回填：查询日志二期结构化字段
-- 说明：
-- 1) 可重复执行，已存在值不会被覆盖
-- 2) 支持按 ID 区间分批执行，降低锁冲击
-- =========================================================

SET @from_id := 0;
SET @to_id := 9223372036854775807;

UPDATE user_api_query_record u
SET
  u.device_id = CASE
    WHEN u.device_id IS NOT NULL AND u.device_id <> '' THEN u.device_id
    WHEN LOCATE('deviceId=', IFNULL(u.request_params, '')) > 0 THEN
      TRIM(BOTH '}' FROM TRIM(BOTH '{' FROM TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(u.request_params, 'deviceId=', -1), ',', 1))))
    ELSE u.device_id
  END,
  u.device_source = CASE
    WHEN u.device_source IS NOT NULL AND u.device_source <> '' THEN u.device_source
    WHEN LOCATE('deviceSource=', IFNULL(u.request_params, '')) > 0 THEN
      TRIM(BOTH '}' FROM TRIM(BOTH '{' FROM TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(u.request_params, 'deviceSource=', -1), ',', 1))))
    WHEN u.query_type = '9' AND u.create_by LIKE 'free-ip-%' AND LOCATE('deviceId=ip#', IFNULL(u.request_params, '')) > 0 THEN 'ip-fallback'
    WHEN u.query_type = '9' AND u.create_by LIKE 'free-ip-%' THEN 'client'
    ELSE u.device_source
  END,
  u.source_type = CASE
    WHEN u.source_type IS NOT NULL AND u.source_type <> '' THEN UPPER(u.source_type)
    WHEN LOCATE('sourceType=', IFNULL(u.request_params, '')) > 0 THEN
      UPPER(TRIM(BOTH '}' FROM TRIM(BOTH '{' FROM TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(u.request_params, 'sourceType=', -1), ',', 1)))))
    WHEN u.query_type = '9' AND u.create_by LIKE 'free-ip-%'
         AND u.task_id IS NOT NULL AND u.task_id <> ''
         AND (LOCATE('deviceId=fqu#', IFNULL(u.request_params, '')) > 0
              OR LOCATE('deviceId=mark-user-', IFNULL(u.request_params, '')) > 0) THEN 'FREE_BATCH'
    WHEN u.query_type = '9' AND u.create_by LIKE 'free-ip-%' THEN 'FREE_SINGLE'
    ELSE u.source_type
  END,
  u.used_before = CASE
    WHEN u.used_before IS NOT NULL THEN u.used_before
    WHEN LOCATE('usedBefore=', IFNULL(u.request_params, '')) > 0 THEN
      CAST(TRIM(BOTH '}' FROM TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(u.request_params, 'usedBefore=', -1), ',', 1))) AS SIGNED)
    ELSE u.used_before
  END,
  u.used_after = CASE
    WHEN u.used_after IS NOT NULL THEN u.used_after
    WHEN LOCATE('usedAfter=', IFNULL(u.request_params, '')) > 0 THEN
      CAST(TRIM(BOTH '}' FROM TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(u.request_params, 'usedAfter=', -1), ',', 1))) AS SIGNED)
    ELSE u.used_after
  END,
  u.limit_value = CASE
    WHEN u.limit_value IS NOT NULL THEN u.limit_value
    WHEN LOCATE('limit=', IFNULL(u.request_params, '')) > 0 THEN
      CAST(TRIM(BOTH '}' FROM TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(u.request_params, 'limit=', -1), ',', 1))) AS SIGNED)
    ELSE u.limit_value
  END,
  u.ip_addr = CASE
    WHEN u.ip_addr IS NOT NULL AND u.ip_addr <> '' THEN u.ip_addr
    WHEN LOCATE('ip=', IFNULL(u.request_params, '')) > 0 THEN
      TRIM(BOTH '}' FROM TRIM(BOTH '{' FROM TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(u.request_params, 'ip=', -1), ',', 1))))
    WHEN LOCATE('sourceIp=', IFNULL(u.request_params, '')) > 0 THEN
      TRIM(BOTH '}' FROM TRIM(BOTH '{' FROM TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(u.request_params, 'sourceIp=', -1), ' ', 1))))
    WHEN u.create_by LIKE 'free-ip-%' THEN SUBSTRING(u.create_by, 9)
    ELSE u.ip_addr
  END,
  u.error_code = CASE
    WHEN u.error_code IS NOT NULL AND u.error_code <> '' THEN u.error_code
    WHEN u.request_status = '1' THEN
      CASE
        WHEN CONCAT(IFNULL(u.results, ''), ' ', IFNULL(u.response_result, '')) LIKE '%平台当日免费额度%'
          OR CONCAT(IFNULL(u.results, ''), ' ', IFNULL(u.response_result, '')) LIKE '%全局%' THEN 'FREE_ALL_LIMIT'
        WHEN CONCAT(IFNULL(u.results, ''), ' ', IFNULL(u.response_result, '')) LIKE '%设备%' THEN 'FREE_DEVICE_LIMIT'
        WHEN CONCAT(IFNULL(u.results, ''), ' ', IFNULL(u.response_result, '')) LIKE '%IP%'
          OR CONCAT(IFNULL(u.results, ''), ' ', IFNULL(u.response_result, '')) LIKE '%ip%' THEN 'FREE_IP_LIMIT'
        ELSE 'FREE_REQUEST_FAIL'
      END
    ELSE u.error_code
  END
WHERE u.id BETWEEN @from_id AND @to_id
  AND (
    u.device_id IS NULL OR u.device_id = ''
    OR u.device_source IS NULL OR u.device_source = ''
    OR u.source_type IS NULL OR u.source_type = ''
    OR u.used_before IS NULL
    OR u.used_after IS NULL
    OR u.limit_value IS NULL
    OR u.ip_addr IS NULL OR u.ip_addr = ''
    OR (u.request_status = '1' AND (u.error_code IS NULL OR u.error_code = ''))
  );

SET FOREIGN_KEY_CHECKS = 1;
