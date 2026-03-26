CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(64),
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 normal 0 disabled',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(64) NOT NULL UNIQUE,
    role_name VARCHAR(64) NOT NULL,
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NOT NULL DEFAULT 0,
    perm_code VARCHAR(128) NOT NULL UNIQUE,
    perm_name VARCHAR(64) NOT NULL,
    perm_type TINYINT NOT NULL COMMENT '1 dir 2 menu 3 button 4 api',
    path VARCHAR(255),
    component VARCHAR(255),
    icon VARCHAR(64),
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL,
    perm_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, perm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_role (id, role_code, role_name, remark) VALUES
(1, 'ADMIN', '超级管理员', '内置角色');

INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, path, component, icon, sort_order) VALUES
(1, 0, 'system', '系统管理', 1, NULL, NULL, 'Setting', 1),
(2, 1, 'system:user:list', '用户管理', 2, '/system/user', 'system/UserList', 'User', 10),
(3, 1, 'system:role:list', '角色管理', 2, '/system/role', 'system/RoleList', 'UserFilled', 20),
(4, 1, 'system:menu:list', '菜单管理', 2, '/system/menu', 'system/MenuList', 'Menu', 30),
(10, 2, 'system:user:add', '用户新增', 4, NULL, NULL, NULL, 1),
(11, 2, 'system:user:edit', '用户编辑', 4, NULL, NULL, NULL, 2),
(12, 2, 'system:user:delete', '用户删除', 4, NULL, NULL, NULL, 3),
(13, 2, 'system:user:resetPwd', '重置密码', 4, NULL, NULL, NULL, 4),
(20, 3, 'system:role:add', '角色新增', 4, NULL, NULL, NULL, 1),
(21, 3, 'system:role:edit', '角色编辑', 4, NULL, NULL, NULL, 2),
(22, 3, 'system:role:delete', '角色删除', 4, NULL, NULL, NULL, 3),
(23, 3, 'system:role:perm', '分配权限', 4, NULL, NULL, NULL, 4),
(30, 4, 'system:menu:add', '菜单新增', 4, NULL, NULL, NULL, 1),
(31, 4, 'system:menu:edit', '菜单编辑', 4, NULL, NULL, NULL, 2),
(32, 4, 'system:menu:delete', '菜单删除', 4, NULL, NULL, NULL, 3);

INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 1, id FROM sys_permission;
