-- 技术方案 §9 / §17：定时任务配置、蓝图导入、外网检索审计查看
INSERT IGNORE INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, path, component, icon, sort_order) VALUES
(221, 200, 'mag:sched:manage', 'MAG 定时任务', 4, NULL, NULL, NULL, 60),
(222, 200, 'mag:kb:blueprint:import', 'MAG 蓝图导入', 4, NULL, NULL, NULL, 61),
(223, 200, 'mag:audit:fetch:view', 'MAG 外网检索审计', 4, NULL, NULL, NULL, 62);

INSERT IGNORE INTO sys_role_permission (role_id, perm_id)
SELECT 1, id FROM sys_permission WHERE id IN (221, 222, 223);
