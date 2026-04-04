CREATE TABLE IF NOT EXISTS mag_orchestration_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    run_kind VARCHAR(16) NOT NULL COMMENT 'AGENT | THREAD',
    agent_id BIGINT NULL,
    thread_id BIGINT NULL,
    workflow_id VARCHAR(192) NULL,
    status VARCHAR(32) NOT NULL COMMENT 'SUBMITTED | RUNNING | SUCCEEDED | FAILED | REJECTED',
    message VARCHAR(512) NULL,
    result_summary VARCHAR(1024) NULL,
    trigger_user_id BIGINT NOT NULL,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME NULL,
    INDEX idx_mag_orch_run_project_started (project_id, started_at DESC),
    UNIQUE KEY uk_mag_orch_run_workflow (workflow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
