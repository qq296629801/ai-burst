package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 核查裁定：PASS → 任务 DONE；FAIL → 回到 IN_PROGRESS。写入 {@code mag_task_verification}。
 */
@Data
public class MagTaskVerifyDecisionRequest {

    /** {@code PASS} 或 {@code FAIL}（大小写不敏感） */
    @NotBlank
    private String result;

    /** 承担核查结论的 VERIFY 角色 Agent id（须属于任务所在项目） */
    @NotNull
    private Long verifierAgentId;

    /** 结论文本（必填） */
    @NotBlank
    private String rationale;

    /** 证据/范围摘要（可选） */
    private String evidenceSummary;

    /** 乐观锁；缺省时使用服务端当前版本 */
    private Integer rowVersion;
}
