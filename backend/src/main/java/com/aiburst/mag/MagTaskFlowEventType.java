package com.aiburst.mag;

/**
 * {@code mag_task_flow_event.event_type} 取值。
 */
public final class MagTaskFlowEventType {

    private MagTaskFlowEventType() {
    }

    public static final String TASK_CREATED = "TASK_CREATED";
    public static final String TASK_DISPATCHED = "TASK_DISPATCHED";
    public static final String TASK_PM_REASSIGNED = "TASK_PM_REASSIGNED";
    public static final String TASK_STARTED = "TASK_STARTED";
    public static final String TASK_SUBMIT_COMPLETE = "TASK_SUBMIT_COMPLETE";
    public static final String TASK_BLOCKED = "TASK_BLOCKED";
    /** 关联任务的 Agent 编排 Activity 失败：进行中 → 阻塞（与手工阻塞区分，便于时间线筛选） */
    public static final String TASK_ORCHESTRATION_FAILED = "TASK_ORCHESTRATION_FAILED";
    public static final String TASK_REQUEST_NEXT = "TASK_REQUEST_NEXT";
    /** 待核查 → 核查中（自动或手工 begin-verify） */
    public static final String TASK_VERIFYING_STARTED = "TASK_VERIFYING_STARTED";
    /** 核查结论通过 → DONE */
    public static final String TASK_VERIFICATION_PASS = "TASK_VERIFICATION_PASS";
    /** 核查结论不通过 → IN_PROGRESS */
    public static final String TASK_VERIFICATION_FAIL = "TASK_VERIFICATION_FAIL";
}
