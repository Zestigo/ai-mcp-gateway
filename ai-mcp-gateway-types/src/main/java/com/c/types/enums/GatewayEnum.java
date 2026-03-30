package com.c.types.enums;

import com.c.types.exception.AppException;
import lombok.Getter;

/**
 * 网关共用枚举
 * 抽取网关相关状态、鉴权等通用枚举类型，统一管理
 *
 * @author cyh
 * @date 2026/03/30
 */
public class GatewayEnum {

    /**
     * 网关校验状态枚举
     */
    @Getter
    public enum GatewayStatus {

        /** 不校验 */
        NOT_VERIFIED(0, "不校验"),

        /** 强校验 */
        STRONG_VERIFIED(1, "强校验");

        /** 状态编码 */
        private final Integer code;

        /** 状态描述 */
        private final String info;

        GatewayStatus(Integer code, String info) {
            this.code = code;
            this.info = info;
        }

        /**
         * 根据编码获取网关校验状态枚举
         *
         * @param code 状态编码
         * @return 匹配的网关校验状态枚举
         */
        public static GatewayStatus get(Integer code) {
            if (code == null) return null;
            for (GatewayStatus val : values()) {
                if (val.code.equals(code)) {
                    return val;
                }
            }
            throw new AppException(ResponseCode.ENUM_NOT_FOUND.getCode(), ResponseCode.ENUM_NOT_FOUND.getInfo());
        }
    }

    /**
     * 网关鉴权状态枚举
     */
    @Getter
    public enum GatewayAuthStatusEnum {

        /** 启用 */
        ENABLE(1, "启用"),

        /** 禁用 */
        DISABLE(0, "禁用");

        /** 鉴权状态编码 */
        private final Integer code;

        /** 鉴权状态描述 */
        private final String info;

        GatewayAuthStatusEnum(Integer code, String info) {
            this.code = code;
            this.info = info;
        }

        /**
         * 根据编码获取网关鉴权状态枚举
         *
         * @param code 状态编码
         * @return 匹配的网关鉴权状态枚举
         */
        public static GatewayAuthStatusEnum getByCode(Integer code) {
            if (null == code) {
                return null;
            }
            for (GatewayAuthStatusEnum anEnum : GatewayAuthStatusEnum.values()) {
                if (anEnum
                        .getCode()
                        .equals(code)) {
                    return anEnum;
                }
            }

            throw new AppException(ResponseCode.ENUM_NOT_FOUND.getCode(), ResponseCode.ENUM_NOT_FOUND.getInfo());
        }

    }

}