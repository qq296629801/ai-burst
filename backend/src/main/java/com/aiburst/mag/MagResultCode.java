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
    MAG_TEMPORAL_START_FAILED(41020, 502, "MAG_TEMPORAL_START_FAILED"),
    MAG_TEMPORAL_QUERY_TIMEOUT(41021, 504, "MAG_TEMPORAL_QUERY_TIMEOUT"),
    MAG_UNKNOWN(41999, 500, "MAG_UNKNOWN");

    private final int code;
    private final int httpStatus;
    private final String defaultMessage;
}
