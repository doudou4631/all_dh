-- Add independent query API configurations for the three Wuma platforms.
-- Copy runtime settings inside the database; do not export credentials into SQL.
-- Existing Wuma configurations are preserved when this script is run again.
SET NAMES utf8mb4;
START TRANSACTION;

CREATE TEMPORARY TABLE tmp_wuma_query_api_mapping (
    source_key varchar(50) NOT NULL,
    target_key varchar(50) NOT NULL PRIMARY KEY,
    target_name varchar(100) NOT NULL,
    sort_num int NOT NULL
);

INSERT INTO tmp_wuma_query_api_mapping (source_key, target_key, target_name, sort_num)
VALUES
    ('td_gaopin', 'wuma_taidixiong', 'Taidixiong无码', 11),
    ('tengxun', 'wuma_tengxun', 'Tengxun无码', 12),
    ('360fugai', 'wuma_360', '360无码', 13);

-- Abort if an existing query API source is missing, duplicated, or disabled.
CREATE TEMPORARY TABLE tmp_wuma_query_api_guard (
    valid tinyint NOT NULL CHECK (valid = 1)
);
INSERT INTO tmp_wuma_query_api_guard (valid)
SELECT IF(COUNT(*) = 3 AND COUNT(DISTINCT mapping.target_key) = 3, 1, 0)
FROM tmp_wuma_query_api_mapping mapping
JOIN user_platform_url_config source
  ON BINARY source.platform_key = BINARY mapping.source_key
WHERE source.status = '0';

INSERT INTO user_platform_url_config (
    platform_id, platform_key, platform_name, url,
    request_interval_ms, timeout_ms, retry_count,
    pre_action_type, pre_action_config, headers_template, concurrency_limit,
    request_params, parse_attr, sort, status,
    free_query_status, free_query_sort, free_query_icon, free_query_description,
    create_by, create_time, update_by, update_time, remark
)
SELECT
    NULL, mapping.target_key, mapping.target_name, source.url,
    source.request_interval_ms, source.timeout_ms, source.retry_count,
    source.pre_action_type, source.pre_action_config, source.headers_template, source.concurrency_limit,
    source.request_params, source.parse_attr, mapping.sort_num, '0',
    '1', NULL, NULL, NULL,
    'admin', NOW(), 'admin', NOW(),
    CONCAT('无码独立检测配置；初始查询参数复制自 ', mapping.source_key)
FROM tmp_wuma_query_api_mapping mapping
JOIN user_platform_url_config source
  ON BINARY source.platform_key = BINARY mapping.source_key
WHERE NOT EXISTS (
    SELECT 1 FROM user_platform_url_config existing
    WHERE BINARY existing.platform_key = BINARY mapping.target_key
);

DROP TEMPORARY TABLE tmp_wuma_query_api_guard;
DROP TEMPORARY TABLE tmp_wuma_query_api_mapping;
COMMIT;
