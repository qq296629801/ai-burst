package com.aiburst.exception;

import com.aiburst.common.ApiResult;
import com.aiburst.common.ResultCode;
import com.aiburst.llm.exception.LlmInvocationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<Void> badCredentials(BadCredentialsException e) {
        return ApiResult.fail(ResultCode.UNAUTHORIZED.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> valid(Exception e) {
        FieldError fe = null;
        if (e instanceof MethodArgumentNotValidException) {
            fe = ((MethodArgumentNotValidException) e).getBindingResult().getFieldError();
        } else if (e instanceof BindException) {
            fe = ((BindException) e).getBindingResult().getFieldError();
        }
        String msg = fe != null ? fe.getDefaultMessage() : ResultCode.BAD_REQUEST.getMessage();
        return ApiResult.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> illegalArg(IllegalArgumentException e) {
        return ApiResult.fail(ResultCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    @ExceptionHandler(LlmInvocationException.class)
    public ResponseEntity<ApiResult<Void>> llmInvoke(LlmInvocationException e) {
        int sc = e.getHttpStatus();
        if (sc < 400 || sc > 599) {
            sc = HttpStatus.BAD_GATEWAY.value();
        }
        return ResponseEntity.status(sc).body(ApiResult.fail(sc, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> other(Exception e) {
        return ApiResult.fail(ResultCode.SERVER_ERROR.getCode(), e.getMessage());
    }
}
