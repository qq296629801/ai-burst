-- 多 Agent 协作菜单与权限种子（§10）；超级管理员角色 id=1 继承全部
-- path/component 供前端路由占位，实现期可改

INSERT IGNORE INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, path, component, icon, sort_order) VALUES
(200, 0, 'mag', '多Agent协作', 1, NULL, NULL, 'Grid', 6),
(201, 200, 'mag:project:list', 'MAG 项目', 2, '/mag/projects', 'mag/ProjectList', 'FolderOpened', 10),
(202, 200, 'mag:dashboard:view', 'MAG 大屏', 2, '/mag/dashboard', 'mag/Dashboard', 'Monitor', 20),
(203, 200, 'mag:pool:decide', 'MAG 待办', 2, '/mag/todos', 'mag/TodoList', 'Bell', 30),
(204, 200, 'mag:kb:manage', 'MAG 知识库', 2, '/mag/kb', 'mag/KbList', 'Reading', 40),
(205, 200, 'mag:release:archive', 'MAG 归档', 2, '/mag/releases', 'mag/ReleaseList', 'Box', 50),
(211, 201, 'mag:project:manage', '项目管理', 4, NULL, NULL, NULL, 1),
(212, 201, 'mag:agent:manage', 'Agent 配置', 4, NULL, NULL, NULL, 2),
(213, 201, 'mag:task:operate', '任务与申报', 4, NULL, NULL, NULL, 3),
(214, 201, 'mag:req:edit', '需求文档', 4, NULL, NULL, NULL, 4),
(216, 202, 'mag:dashboard:org', '组织级大屏', 4, NULL, NULL, NULL, 1);

INSERT IGNORE INTO sys_role_permission (role_id, perm_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 200 AND 220;
