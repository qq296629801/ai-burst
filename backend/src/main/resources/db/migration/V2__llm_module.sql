CREATE TABLE IF NOT EXISTS llm_channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    provider_code VARCHAR(32) NOT NULL,
    protocol VARCHAR(32) NOT NULL,
    channel_name VARCHAR(64) NOT NULL,
    base_url VARCHAR(512) NOT NULL,
    api_key_cipher TEXT NOT NULL,
    default_model VARCHAR(128),
    extra_json VARCHAR(1024),
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_llm_channel_owner (owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, path, component, icon, sort_order) VALUES
(100, 0, 'llm', '大模型', 1, NULL, NULL, 'Cpu', 5),
(101, 100, 'llm:channel:list', '通道配置', 2, '/llm/channel', 'llm/ChannelList', 'Connection', 10),
(102, 100, 'llm:playground:list', '对话体验', 2, '/llm/playground', 'llm/Playground', 'ChatDotRound', 20),
(110, 101, 'llm:channel:add', '通道新增', 4, NULL, NULL, NULL, 1),
(111, 101, 'llm:channel:edit', '通道编辑', 4, NULL, NULL, NULL, 2),
(112, 101, 'llm:channel:delete', '通道删除', 4, NULL, NULL, NULL, 3),
(120, 102, 'llm:chat:invoke', '发起对话', 4, NULL, NULL, NULL, 1);

INSERT IGNORE INTO sys_role_permission (role_id, perm_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 100 AND 120;
