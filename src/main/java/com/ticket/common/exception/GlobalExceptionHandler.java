package com.ticket.common.exception;

import com.ticket.common.result.R;
import com.ticket.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e, HttpServletResponse response) {
        log.warn("业务异常: {}", e.getMessage());
        if (e.getCode() != null && e.getCode() == ResultCode.UNAUTHORIZED.getCode()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else if (e.getCode() != null && e.getCode() == ResultCode.FORBIDDEN.getCode()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletResponse response) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return R.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e, HttpServletResponse response) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", msg);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return R.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletResponse response) {
        log.warn("约束校验失败: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return R.fail(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletResponse response) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return R.fail(ResultCode.PARAM_ERROR.getCode(), "缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletResponse response) {
        log.warn("参数类型不匹配: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return R.fail(ResultCode.PARAM_ERROR.getCode(), "参数类型不匹配");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        log.error("系统异常", e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return R.fail(ResultCode.INTERNAL_ERROR);
    }
}
