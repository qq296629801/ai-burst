-- Remove features: requirement pool, release archive, PM assist, agent improvement log.
-- KB rows tied to release archives are removed first (no FK, archive_id is loose reference).

DELETE FROM mag_kb_entry WHERE archive_id IS NOT NULL OR source = 'ARCHIVE_REFLOW';

DROP TABLE IF EXISTS mag_pm_assist_record;
DROP TABLE IF EXISTS mag_agent_improvement_log;
DROP TABLE IF EXISTS mag_requirement_pool_item;
DROP TABLE IF EXISTS mag_release_archive;

DELETE rp FROM sys_role_permission rp
    INNER JOIN sys_permission p ON rp.perm_id = p.id
    WHERE p.perm_code IN ('mag:pool:decide', 'mag:release:archive');

DELETE FROM sys_permission WHERE perm_code IN ('mag:pool:decide', 'mag:release:archive');
