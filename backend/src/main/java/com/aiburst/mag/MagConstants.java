package com.aiburst.mag;

/** MAG 域内共享常量。 */
public final class MagConstants {

    private MagConstants() {
    }

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String ROLE_VIEWER = "VIEWER";

    public static final String TASK_PENDING = "PENDING";
    public static final String TASK_IN_PROGRESS = "IN_PROGRESS";
    public static final String TASK_DONE = "DONE";
    public static final String TASK_BLOCKED = "BLOCKED";

    public static final String KB_SOURCE_MANUAL = "MANUAL";
    public static final String KB_SOURCE_ARCHIVE_REFLOW = "ARCHIVE_REFLOW";

    public static final String ORCH_RUN_KIND_AGENT = "AGENT";
    public static final String ORCH_RUN_KIND_THREAD = "THREAD";

    public static final String ORCH_STATUS_SUBMITTED = "SUBMITTED";
    public static final String ORCH_STATUS_RUNNING = "RUNNING";
    public static final String ORCH_STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String ORCH_STATUS_FAILED = "FAILED";
    public static final String ORCH_STATUS_REJECTED = "REJECTED";

    /** 任务执行记录：Temporal 触发被拒绝（未进入活动），与编排行 {@link #ORCH_STATUS_REJECTED} 对应但单独命名便于查询 */
    public static final String EXECUTION_OUTCOME_TRIGGER_REJECTED = "TRIGGER_REJECTED";
}
