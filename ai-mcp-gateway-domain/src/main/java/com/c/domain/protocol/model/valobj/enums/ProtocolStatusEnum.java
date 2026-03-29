package com.c.domain.protocol.model.valobj.enums;

import com.c.types.enums.ResponseCode;
import com.c.types.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 协议状态枚举
 * 定义协议启用、禁用两种状态
 *
 * @author cyh
 * @date 2026/03/29
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ProtocolStatusEnum {

    /** 启用状态 */
    ENABLE(1, "启用"),

    /** 禁用状态 */
    DISABLE(0, "禁用");

    /** 状态编码 */
    private Integer code;

    /** 状态描述 */
    private String info;

    /**
     * 根据状态编码获取对应的枚举对象
     *
     * @param code 状态编码
     * @return 协议状态枚举
     * @throws AppException 未找到对应枚举时抛出异常
     */
    public static ProtocolStatusEnum getByCode(Integer code) {
        if (null == code) {
            return null;
        }
        for (ProtocolStatusEnum anEnum : ProtocolStatusEnum.values()) {
            if (anEnum
                    .getCode()
                    .equals(code)) {
                return anEnum;
            }
        }

        throw new AppException(ResponseCode.ENUM_NOT_FOUND.getCode(), ResponseCode.ENUM_NOT_FOUND.getInfo());
    }

}