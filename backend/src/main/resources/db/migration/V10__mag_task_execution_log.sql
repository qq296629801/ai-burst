-- 任务关联 Agent 编排每次终态（成功/失败/触发被拒）留痕，便于审计与界面「执行记录」
CREATE TABLE IF NOT EXISTS mag_task_execution_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    orchestration_run_id BIGINT NOT NULL,
    workflow_id VARCHAR(192) NULL,
    execution_outcome VARCHAR(32) NOT NULL COMMENT 'SUCCEEDED | FAILED | TRIGGER_REJECTED',
    result_summary VARCHAR(1024) NULL,
    trigger_user_id BIGINT NOT NULL,
    started_at DATETIME NOT NULL,
    finished_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_mag_task_exec_task_time (task_id, finished_at DESC),
    KEY idx_mag_task_exec_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
