package com.aiburst.mag.dto;

import lombok.Data;

/**
 * 可选携带 rowVersion；为空时使用服务端当前版本尝试更新（仍可能 409）。
 */
@Data
public class MagSubmitCompleteRequest {

    private Integer rowVersion;
}
