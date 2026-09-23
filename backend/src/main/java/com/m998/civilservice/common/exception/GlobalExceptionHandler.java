package com.m998.civilservice.common.exception;

import com.m998.civilservice.common.api.CommonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = ApiException.class)
    public CommonResult handle(ApiException e) {
        LOGGER.warn("业务异常: {}", e.getMessage());
        if (e.getErrorCode() != null) {
            return CommonResult.failed(e.getErrorCode());
        }
        return CommonResult.failed(e.getMessage());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public CommonResult handleValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = null;
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                message = fieldError.getField() + fieldError.getDefaultMessage();
            }
        }
        LOGGER.warn("参数校验失败: {}", message);
        return CommonResult.validateFailed(message);
    }

    @ExceptionHandler(value = BindException.class)
    public CommonResult handleValidException(BindException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = null;
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                message = fieldError.getField() + fieldError.getDefaultMessage();
            }
        }
        LOGGER.warn("参数绑定失败: {}", message);
        return CommonResult.validateFailed(message);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public CommonResult handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        LOGGER.warn("请求体解析失败: {}", e.getMessage());
        return CommonResult.validateFailed("请求体格式错误，请检查JSON格式");
    }

    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public CommonResult handleMissingParam(MissingServletRequestParameterException e) {
        LOGGER.warn("缺少请求参数: {}", e.getParameterName());
        return CommonResult.validateFailed("缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public CommonResult handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        LOGGER.warn("不支持的请求方法: {}", e.getMethod());
        return CommonResult.failed("不支持的请求方法: " + e.getMethod());
    }

    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    public CommonResult handleMaxUploadSize(MaxUploadSizeExceededException e) {
        LOGGER.warn("上传文件大小超出限制: {}", e.getMessage());
        return CommonResult.validateFailed("文件大小超出限制");
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public CommonResult handleAccessDenied(AccessDeniedException e) {
        LOGGER.warn("访问被拒绝: {}", e.getMessage());
        return CommonResult.forbidden(null);
    }

    @ExceptionHandler(value = Exception.class)
    public CommonResult handleException(Exception e) {
        LOGGER.error("系统异常", e);
        return CommonResult.failed("系统繁忙，请稍后再试");
    }
}
