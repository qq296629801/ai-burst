package com.aiburst.llm.exception;

import lombok.Getter;

@Getter
public class LlmInvocationException extends RuntimeException {

    private final int httpStatus;

    public LlmInvocationException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public LlmInvocationException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = 502;
    }
}
