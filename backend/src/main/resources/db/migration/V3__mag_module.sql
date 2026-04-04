-- 多 Agent 协作领域表（mag_*）
-- 设计说明见：技术方案/多Agent协作技术方案.md §15、§4

CREATE TABLE mag_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 active 0 archived',
    config_json JSON NULL COMMENT 'thresholds, feature flags',
    current_req_doc_id BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_project_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_in_project VARCHAR(32) NOT NULL COMMENT 'OWNER, MEMBER, VIEWER（不设仅拍板专用角色）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_proj_user (project_id, user_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_agent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    parent_agent_id BIGINT NULL,
    role_type VARCHAR(32) NOT NULL COMMENT 'PM,PRODUCT,BACKEND,FRONTEND,TEST,VERIFY',
    name VARCHAR(128) NOT NULL,
    llm_channel_id BIGINT NULL COMMENT 'llm_channel.id',
    system_prompt_profile VARCHAR(64) NULL,
    extra_json JSON NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_project (project_id),
    KEY idx_parent (parent_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_agent_improvement_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    change_type VARCHAR(32) NOT NULL COMMENT 'CONFIG,PROMPT,FEEDBACK,QUALITY_TAG,...',
    summary VARCHAR(512) NOT NULL,
    detail_json JSON NULL,
    created_by_user_id BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_agent (agent_id, created_at),
    KEY idx_project (project_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_module (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(256) NOT NULL,
    tag VARCHAR(64) NULL,
    sort_order INT DEFAULT 0,
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    module_id BIGINT NULL,
    title VARCHAR(256) NOT NULL,
    description TEXT NULL,
    state VARCHAR(32) NOT NULL COMMENT 'PENDING,IN_PROGRESS,PENDING_VERIFY,VERIFYING,DONE,BLOCKED',
    assignee_agent_id BIGINT NULL,
    reporter_agent_id BIGINT NULL,
    requirement_ref JSON NULL,
    temporal_workflow_id VARCHAR(256) NULL,
    block_reason VARCHAR(512) NULL,
    blocked_by_agent_id BIGINT NULL,
    row_version INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_project_state (project_id, state),
    KEY idx_assignee (assignee_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_task_verification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    result VARCHAR(16) NOT NULL COMMENT 'PASS,FAIL',
    verifier_agent_id BIGINT NOT NULL,
    rationale TEXT NOT NULL,
    evidence_summary TEXT NULL,
    search_trace_json JSON NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_task (task_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_thread (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    title VARCHAR(256) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    thread_id BIGINT NOT NULL,
    sender_type VARCHAR(16) NOT NULL COMMENT 'USER,AGENT,SYSTEM',
    sender_agent_id BIGINT NULL,
    content MEDIUMTEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_thread (thread_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_requirement_doc (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL UNIQUE,
    current_version INT NOT NULL DEFAULT 0,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_requirement_revision (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id BIGINT NOT NULL,
    version INT NOT NULL,
    content MEDIUMTEXT NOT NULL,
    author_user_id BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doc_ver (doc_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_requirement_pool_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    state VARCHAR(32) NOT NULL,
    revision_id BIGINT NULL,
    anchor_json JSON NULL,
    payload_json JSON NULL,
    assigned_decider_user_id BIGINT NULL COMMENT '空=具备 mag:pool:decide 的成员可见；非空=仅该用户与 OWNER',
    temporal_workflow_id VARCHAR(256) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_project_state (project_id, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_release_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    version_label VARCHAR(64) NOT NULL,
    snapshot_json JSON NULL,
    minio_object_key VARCHAR(512) NULL,
    quality_flag TINYINT NOT NULL DEFAULT 0 COMMENT '1 eligible for kb reflow',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_kb_entry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source VARCHAR(32) NOT NULL COMMENT 'ARCHIVE_REFLOW,MANUAL',
    archive_id BIGINT NULL,
    title VARCHAR(256) NOT NULL,
    body MEDIUMTEXT NOT NULL,
    tags_json JSON NULL,
    keywords VARCHAR(512) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FULLTEXT KEY ft_title_body (title, body),
    KEY idx_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_pm_assist_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    problem_type VARCHAR(64) NULL,
    root_cause_summary TEXT NULL,
    action_taken TEXT NULL,
    assisted_agent_ids_json JSON NULL,
    resolved TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_alert_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NULL,
    task_id BIGINT NULL,
    alert_type VARCHAR(64) NOT NULL,
    level VARCHAR(16) NOT NULL DEFAULT 'INFO',
    payload_json JSON NULL,
    acknowledged TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project_time (project_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_external_fetch_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NULL,
    user_id BIGINT NULL,
    normalized_url VARCHAR(1024) NOT NULL,
    http_status INT NULL,
    body_hash VARCHAR(64) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mag_scheduled_job_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_key VARCHAR(64) NOT NULL UNIQUE,
    cron_expr VARCHAR(64) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    project_id BIGINT NULL,
    last_run_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 可选：新建项目后在应用层创建 mag_requirement_doc 并回写 mag_project.current_req_doc_id
