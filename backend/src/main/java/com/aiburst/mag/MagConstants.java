package com.aiburst.mag;

/**
 * 与《多Agent协作技术方案》§19.1 对齐的常量。
 */
public final class MagConstants {

    private MagConstants() {
    }

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String ROLE_VIEWER = "VIEWER";

    public static final String TASK_PENDING = "PENDING";
    public static final String TASK_IN_PROGRESS = "IN_PROGRESS";
    public static final String TASK_PENDING_VERIFY = "PENDING_VERIFY";
    public static final String TASK_VERIFYING = "VERIFYING";
    public static final String TASK_DONE = "DONE";
    public static final String TASK_BLOCKED = "BLOCKED";

    /** 待用户拍板（§16.2） */
    public static final String POOL_PENDING_USER = "PENDING_USER";

    /** 产品 §6.3 需求池状态（与 §19.1 扩展一致） */
    public static final String POOL_CLOSED_BY_PRODUCT = "CLOSED_BY_PRODUCT";
    public static final String USER_CONFIRMED_OK = "USER_CONFIRMED_OK";
    public static final String USER_CONFIRMED_CHANGE = "USER_CONFIRMED_CHANGE";

    public static final String KB_SOURCE_MANUAL = "MANUAL";
    public static final String KB_SOURCE_ARCHIVE_REFLOW = "ARCHIVE_REFLOW";

    public static final String DECISION_APPROVE_AS_IS = "APPROVE_AS_IS";
    public static final String DECISION_APPROVE_WITH_CHANGE = "APPROVE_WITH_CHANGE";
    public static final String DECISION_REJECT = "REJECT";
    public static final String DECISION_DEFER = "DEFER";
}
