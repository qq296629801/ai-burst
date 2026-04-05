-- 移除外网抓取审计表及查看权限（产品不再保留外网审计能力）
DROP TABLE IF EXISTS mag_external_fetch_audit;

DELETE FROM sys_role_permission WHERE perm_id = 223;
DELETE FROM sys_permission WHERE id = 223 OR perm_code = 'mag:audit:fetch:view';
