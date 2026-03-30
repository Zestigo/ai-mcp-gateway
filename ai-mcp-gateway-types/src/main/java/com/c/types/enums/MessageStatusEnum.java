package com.c.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 本地消息表状态枚举
 * 用于分布式事务最终一致性方案（事务库模式）
 *
 * @author cyh
 * @date 2026/03/30
 */
@Getter
@AllArgsConstructor
public enum MessageStatusEnum {

    /** 待发送：已记录到数据库，但尚未成功投递到 MQ */
    WAIT("WAIT", "待发送"),

    /** 发送成功：已确认成功投递到 MQ，流程结束 */
    SUCCESS("SUCCESS", "发送成功"),

    /** 发送失败：投递 MQ 过程发生异常，等待定时任务补偿重试 */
    FAIL("FAIL", "发送失败"),

    /** 达到重试上限：多次重试仍失败，需人工接入排查（如死信、消息体过大等） */
    OVER_LIMIT("OVER_LIMIT", "重试上限");

    /** 状态代码 */
    private final String code;

    /** 状态描述 */
    private final String info;

    /**
     * 根据状态代码获取对应的枚举对象
     *
     * @param code 状态代码
     * @return 匹配的枚举对象，无匹配项时返回null
     */
    public static MessageStatusEnum getByCode(String code) {
        for (MessageStatusEnum status : MessageStatusEnum.values()) {
            if (status
                    .getCode()
                    .equals(code)) {
                return status;
            }
        }
        return null;
    }
}