package com.macro.mall.tiny.common.api;

/**
 * 枚举了一些常用API操作码
 * Created by macro on 2019/4/19.
 */
public enum ResultCode implements IErrorCode {
    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数检验失败"),
    UNAUTHORIZED(401, "暂未登录或token已经过期"),
    FORBIDDEN(403, "没有相关权限"),
    // 业务错误码
    IMPORT_SESSION_EXPIRED(1001, "导入会话不存在或已过期"),
    IMPORT_FILE_INVALID(1002, "导入文件格式无效"),
    MATCH_ARCHIVE_MISSING(1003, "用户档案不存在，请先完善个人信息"),
    USER_DUPLICATE(1004, "用户名已存在"),
    PASSWORD_MISMATCH(1005, "旧密码不正确"),
    PASSWORD_CONFIRM_MISMATCH(1006, "两次输入密码不一致"),
    EMAIL_NOT_FOUND(1007, "该邮箱未绑定任何用户"),
    VERIFY_CODE_INVALID(1008, "验证码错误或已过期");
    private long code;
    private String message;

    private ResultCode(long code, String message) {
        this.code = code;
        this.message = message;
    }

    public long getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
