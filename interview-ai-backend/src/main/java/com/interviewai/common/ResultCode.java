package com.interviewai.common;

import lombok.Getter;

/**
 * 统一响应状态码。
 * 设计思路：按模块分段，方便排查问题来源。
 * 1xxx 用户模块, 2xxx 简历模块, 3xxx 面试模块, 4xxx 题库模块, 8xxx AI相关, 9xxx 通用错误
 */
@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数有误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户模块 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "用户名或密码错误"),
    USER_ALREADY_EXISTS(1003, "用户名已被注册"),
    USER_DISABLED(1004, "账号已被禁用"),
    USER_OLD_PASSWORD_ERROR(1005, "旧密码错误"),
    USER_PASSWORD_SAME(1006, "新密码不能与旧密码相同"),

    // 简历模块 2xxx
    RESUME_NOT_FOUND(2001, "简历不存在"),
    RESUME_UPLOAD_FAILED(2002, "简历上传失败"),
    RESUME_PARSE_FAILED(2003, "简历解析失败"),

    // 面试模块 3xxx
    INTERVIEW_NOT_FOUND(3001, "面试场次不存在"),
    INTERVIEW_QUOTA_EXCEEDED(3002, "今日面试次数已用完"),
    INTERVIEW_SESSION_EXPIRED(3003, "面试会话已过期"),

    // 题库模块 4xxx
    QUESTION_NOT_FOUND(4001, "题目不存在"),

    // AI 相关 8xxx
    AI_SERVICE_ERROR(8001, "AI服务调用失败，请稍后重试"),
    AI_TIMEOUT(8002, "AI服务响应超时");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
