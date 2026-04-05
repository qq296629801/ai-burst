-- 任务流程可观测：派工、改派、开始、阻塞、要活、申报完成等事件时间线
CREATE TABLE mag_task_flow_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL COMMENT 'TASK_DISPATCHED, TASK_STARTED, ...',
    actor_type VARCHAR(32) NOT NULL COMMENT 'USER, AGENT',
    actor_agent_id BIGINT NULL,
    summary VARCHAR(512) NULL,
    detail_json MEDIUMTEXT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_task_time (task_id, created_at),
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
