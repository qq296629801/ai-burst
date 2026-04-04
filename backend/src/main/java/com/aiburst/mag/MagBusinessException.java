package com.aiburst.mag;

import lombok.Getter;

/**
 * MAG 域可预期业务异常，由 {@link com.aiburst.exception.GlobalExceptionHandler} 映射为 {@link com.aiburst.common.ApiResult}。
 */
@Getter
public class MagBusinessException extends RuntimeException {

    private final MagResultCode resultCode;

    public MagBusinessException(MagResultCode resultCode) {
        super(resultCode.getDefaultMessage());
        this.resultCode = resultCode;
    }

    public MagBusinessException(MagResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public MagBusinessException(MagResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }
}
