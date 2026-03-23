package com.c.types.exception;

import com.c.types.enums.ResponseCode;
import lombok.Getter;

/**
 * 自定义应用异常
 *
 * @author cyh
 * @date 2026/03/20
 */
@Getter
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 5317680961212299217L;

    /** 异常码 */
    private final String code;

    /** 异常信息 */
    private final String info;

    /**
     * 最推荐用法：直接传入枚举
     * 调用：throw new AppException(ResponseCode.ILLEGAL_PARAMETER);
     */
    public AppException(ResponseCode responseCode) {
        super(responseCode.getInfo());
        this.code = responseCode.getCode();
        this.info = responseCode.getInfo();
    }

    /**
     * 携带异常链的构造
     * 调用：throw new AppException(ResponseCode.UN_ERROR, e);
     */
    public AppException(ResponseCode responseCode, Throwable cause) {
        super(responseCode.getInfo(), cause);
        this.code = responseCode.getCode();
        this.info = responseCode.getInfo();
    }

    /**
     * 灵活扩展：允许覆盖枚举的 info 描述
     * 调用：throw new AppException(ResponseCode.ILLEGAL_PARAMETER, "手机号不合法");
     */
    public AppException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
        this.info = message;
    }

    /**
     * 底层全参构造（设为私有或受保护，强制外部优先使用枚举构造）
     */
    public AppException(String code, String info) {
        super(info);
        this.code = code;
        this.info = info;
    }


    @Override
    public String toString() {
        return "AppException{" +
                "code='" + code + '\'' +
                ", info='" + info + '\'' +
                '}';
    }
}