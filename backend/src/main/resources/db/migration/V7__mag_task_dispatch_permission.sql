-- 项目经理派工：创建任务并指定执行 Agent；与产品 §4.2 协调链一致
INSERT IGNORE INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, path, component, icon, sort_order) VALUES
(224, 201, 'mag:task:dispatch', 'MAG 项目经理派工', 4, NULL, NULL, NULL, 3);

-- 拥有「任务与申报」的角色同步获得派工（与首期工作台一致）
INSERT IGNORE INTO sys_role_permission (role_id, perm_id)
SELECT rp.role_id, 224 FROM sys_role_permission rp WHERE rp.perm_id = 213;

INSERT IGNORE INTO sys_role_permission (role_id, perm_id) VALUES (1, 224);
