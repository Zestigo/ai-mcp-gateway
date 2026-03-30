package com.c.types.exception;

import com.c.types.enums.ResponseCode;
import lombok.Getter;

/**
 * 自定义应用异常
 * 系统统一业务异常类，基于响应码枚举封装异常信息，支持异常链传递与自定义描述
 *
 * @author cyh
 * @date 2026/03/30
 */
@Getter
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 5317680961212299217L;

    /** 异常状态码 */
    private final String code;

    /** 异常描述信息 */
    private final String info;

    /**
     * 构造应用异常（推荐使用）
     *
     * @param responseCode 响应码枚举
     */
    public AppException(ResponseCode responseCode) {
        super(responseCode.getInfo());
        this.code = responseCode.getCode();
        this.info = responseCode.getInfo();
    }

    /**
     * 构造携带异常堆栈信息的应用异常
     *
     * @param responseCode 响应码枚举
     * @param cause        原始异常对象
     */
    public AppException(ResponseCode responseCode, Throwable cause) {
        super(responseCode.getInfo(), cause);
        this.code = responseCode.getCode();
        this.info = responseCode.getInfo();
    }

    /**
     * 构造覆盖自定义描述的应用异常
     *
     * @param responseCode 响应码枚举
     * @param message      自定义异常信息
     */
    public AppException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
        this.info = message;
    }

    /**
     * 全参数构造应用异常
     *
     * @param code 异常状态码
     * @param info 异常描述信息
     */
    public AppException(String code, String info) {
        super(info);
        this.code = code;
        this.info = info;
    }

    /**
     * 重写异常信息输出格式
     *
     * @return 异常对象字符串
     */
    @Override
    public String toString() {
        return "AppException{" + "code='" + code + '\'' + ", info='" + info + '\'' + '}';
    }
}