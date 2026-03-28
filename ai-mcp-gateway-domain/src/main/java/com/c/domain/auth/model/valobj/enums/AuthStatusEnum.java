package com.c.domain.auth.model.valobj.enums;

import com.c.types.enums.ResponseCode;
import com.c.types.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 认证相关配置枚举集合
 * 包含网关校验策略、认证功能开关等枚举定义
 *
 * @author cyh
 * @date 2026/03/27
 */
public class AuthStatusEnum {

    /**
     * 网关校验配置枚举
     * 定义网关请求的校验模式类型
     */
    @Getter
    @AllArgsConstructor
    public enum GatewayConfig {

        /** 不校验 */
        NOT_VERIFIED(0, "不校验"),
        /** 强校验 */
        STRONG_VERIFIED(1, "强校验");

        /** 校验类型编码 */
        private final Integer code;
        /** 校验类型描述 */
        private final String info;

        /**
         * 根据编码获取网关校验配置枚举
         *
         * @param code 枚举编码
         * @return 匹配的网关校验配置枚举
         * @throws AppException 未找到对应枚举时抛出
         */
        public static GatewayConfig get(Integer code) {
            return Arrays
                    .stream(values())
                    .filter(val -> val.code.equals(code))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ResponseCode.ENUM_NOT_FOUND));
        }

        /**
         * 判断是否开启强校验模式
         *
         * @return 强校验返回true，否则返回false
         */
        public boolean isEnabled() {
            return STRONG_VERIFIED.equals(this);
        }
    }

    /**
     * 认证开关配置枚举
     * 定义认证功能的启用/禁用状态
     */
    @Getter
    @AllArgsConstructor
    public enum AuthConfig {

        /** 禁用 */
        DISABLE(0, "禁用"),
        /** 启用 */
        ENABLE(1, "启用");

        /** 状态编码 */
        private final Integer code;
        /** 状态描述 */
        private final String info;

        /**
         * 根据编码获取认证开关配置枚举
         *
         * @param code 枚举编码
         * @return 匹配的认证开关配置枚举
         * @throws AppException 未找到对应枚举时抛出
         */
        public static AuthConfig get(Integer code) {
            return Arrays
                    .stream(values())
                    .filter(val -> val.code.equals(code))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ResponseCode.ENUM_NOT_FOUND));
        }

        /**
         * 判断是否启用认证
         *
         * @return 启用返回true，禁用返回false
         */
        public boolean isEnable() {
            return ENABLE.equals(this);
        }
    }
}