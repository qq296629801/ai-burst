package com.aiburst.mag;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 多 Agent 域业务码，与《多Agent协作技术方案》§19.7 对齐；{@code ApiResult.code} 使用本枚举数值。
 */
@Getter
@RequiredArgsConstructor
public enum MagResultCode {

    MAG_FORBIDDEN(41001, 403, "MAG_FORBIDDEN"),
    MAG_NOT_PROJECT_MEMBER(41002, 403, "MAG_NOT_PROJECT_MEMBER"),
    MAG_NOT_FOUND(41003, 404, "MAG_NOT_FOUND"),
    MAG_TASK_STATE_INVALID(41010, 409, "MAG_TASK_STATE_INVALID"),
    MAG_ROW_VERSION_CONFLICT(41011, 409, "MAG_ROW_VERSION_CONFLICT"),
    /** Agent 触发编排前须绑定大模型通道（llm_channel_id） */
    MAG_AGENT_LLM_CHANNEL_REQUIRED(41015, 400, "MAG_AGENT_LLM_CHANNEL_REQUIRED"),
    /** 需求正文未就绪时不得向非产品职能派工，或主 Agent 向 PM 要活越权 */
    MAG_DISPATCH_REQUIREMENT_NOT_READY(41016, 409, "MAG_DISPATCH_REQUIREMENT_NOT_READY"),
    /** 产品职能尚有未结项任务时不得再向产品 Agent 派工 */
    MAG_DISPATCH_PRODUCT_PIPELINE_BLOCKED(41017, 409, "MAG_DISPATCH_PRODUCT_PIPELINE_BLOCKED"),
    /** 开发职能尚有未结项任务时不得向测试派工或测试向 PM 要活 */
    MAG_DISPATCH_TEST_BLOCKED_BY_DEV(41018, 409, "MAG_DISPATCH_TEST_BLOCKED_BY_DEV"),
    /** 后端尚有未结项任务时不得向前端派工或前端主 Agent 向 PM 要活 */
    MAG_DISPATCH_FRONTEND_BLOCKED_BY_BACKEND(41019, 409, "MAG_DISPATCH_FRONTEND_BLOCKED_BY_BACKEND"),
    MAG_TEMPORAL_START_FAILED(41020, 502, "MAG_TEMPORAL_START_FAILED"),
    MAG_TEMPORAL_QUERY_TIMEOUT(41021, 504, "MAG_TEMPORAL_QUERY_TIMEOUT"),
    MAG_UNKNOWN(41999, 500, "MAG_UNKNOWN");

    private final int code;
    private final int httpStatus;
    private final String defaultMessage;
}
