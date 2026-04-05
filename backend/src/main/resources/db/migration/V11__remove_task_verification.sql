-- 移除核查 Agent：删除核查记录表；历史待核查/核查中任务统一视为已完成；原 VERIFY 角色实例改为 TEST（请在界面核对职能说明）
DROP TABLE IF EXISTS mag_task_verification;

UPDATE mag_task SET state = 'DONE', temporal_workflow_id = NULL
WHERE state IN ('PENDING_VERIFY', 'VERIFYING');

UPDATE mag_agent SET role_type = 'TEST' WHERE role_type = 'VERIFY';
