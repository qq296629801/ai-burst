-- 派工自动执行等场景：编排记录可选关联任务，便于失败时告警定位
ALTER TABLE mag_orchestration_run
    ADD COLUMN task_id BIGINT NULL COMMENT '关联 mag_task.id（可选）' AFTER thread_id;

CREATE INDEX idx_mag_orch_run_task ON mag_orchestration_run (task_id);
