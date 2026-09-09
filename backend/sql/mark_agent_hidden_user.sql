CREATE TABLE IF NOT EXISTS mark_agent_hidden_user (
  agent_user_id BIGINT NOT NULL COMMENT 'agent user id',
  hidden_user_id BIGINT NOT NULL COMMENT 'hidden user id',
  create_by VARCHAR(64) DEFAULT NULL COMMENT 'created by',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  PRIMARY KEY (agent_user_id, hidden_user_id),
  KEY idx_hidden_user_id (hidden_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='agent process hidden users';
