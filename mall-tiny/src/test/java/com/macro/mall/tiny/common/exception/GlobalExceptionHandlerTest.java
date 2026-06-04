package com.macro.mall.tiny.common.exception;

import com.macro.mall.tiny.common.api.CommonResult;
import com.macro.mall.tiny.common.api.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 全局异常处理器单元测试
 */
@DisplayName("GlobalExceptionHandler 单元测试")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ApiException(message) - 返回500 + 消息")
    void apiException_withMessage_returns500() {
        ApiException ex = new ApiException("业务错误");
        CommonResult result = handler.handle(ex);
        assertThat(result.getCode()).isEqualTo(ResultCode.FAILED.getCode());
        assertThat(result.getMessage()).isEqualTo("业务错误");
    }

    @Test
    @DisplayName("ApiException(errorCode) - 返回对应错误码")
    void apiException_withErrorCode_returnsCode() {
        ApiException ex = new ApiException(ResultCode.UNAUTHORIZED);
        CommonResult result = handler.handle(ex);
        assertThat(result.getCode()).isEqualTo(401L);
        assertThat(result.getMessage()).isEqualTo("暂未登录或token已经过期");
    }

    @Test
    @DisplayName("ApiException(IMPORT_SESSION_EXPIRED) - 返回1001")
    void apiException_importSessionExpired_returns1001() {
        ApiException ex = new ApiException(ResultCode.IMPORT_SESSION_EXPIRED);
        CommonResult result = handler.handle(ex);
        assertThat(result.getCode()).isEqualTo(1001L);
        assertThat(result.getMessage()).contains("导入会话");
    }

    @Test
    @DisplayName("BindException - 返回400 + 字段错误信息")
    void bindException_returns400() {
        Object target = new Object();
        org.springframework.validation.BeanPropertyBindingResult bindingResult =
                new org.springframework.validation.BeanPropertyBindingResult(target, "obj");
        bindingResult.addError(new FieldError("obj", "username", "不能为空"));
        BindException ex = new BindException(bindingResult);

        CommonResult result = handler.handleValidException(ex);
        assertThat(result.getCode()).isEqualTo(400L);
        assertThat(result.getMessage()).contains("username");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException - 返回400 + 固定消息")
    void messageNotReadable_returns400() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("JSON parse error");

        CommonResult result = handler.handleHttpMessageNotReadable(ex);
        assertThat(result.getCode()).isEqualTo(400L);
        assertThat(result.getMessage()).isEqualTo("请求体格式错误，请检查JSON格式");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException - 返回400 + 参数名")
    void missingParam_returns400() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("file", "MultipartFile");

        CommonResult result = handler.handleMissingParam(ex);
        assertThat(result.getCode()).isEqualTo(400L);
        assertThat(result.getMessage()).contains("file");
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException - 返回500 + 方法名")
    void methodNotSupported_returnsFailed() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("GET");

        CommonResult result = handler.handleMethodNotSupported(ex);
        assertThat(result.getCode()).isEqualTo(500L);
        assertThat(result.getMessage()).contains("GET");
    }

    @Test
    @DisplayName("AccessDeniedException - 返回403")
    void accessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        CommonResult result = handler.handleAccessDenied(ex);
        assertThat(result.getCode()).isEqualTo(403L);
    }

    @Test
    @DisplayName("通用Exception - 返回500 + 不泄露内部信息")
    void genericException_returns500Generic() {
        NullPointerException ex = new NullPointerException("internal.Class.method at line 42");

        CommonResult result = handler.handleException(ex);
        assertThat(result.getCode()).isEqualTo(500L);
        assertThat(result.getMessage()).isEqualTo("系统繁忙，请稍后再试");
        // 确保不泄露内部信息
        assertThat(result.getMessage()).doesNotContain("NullPointer");
        assertThat(result.getMessage()).doesNotContain("internal");
    }
}
