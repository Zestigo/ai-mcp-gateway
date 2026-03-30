package com.c.domain.protocol.model.valobj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 领域事件动作枚举
 * 用于标识网关配置变更、协议更新等异步通知的类型
 *
 * @author cyh
 * @date 2026/03/29
 */
@Getter
@AllArgsConstructor
public enum EventActionEnum {

    /** 增量刷新：仅更新指定的协议或配置标识 */
    REFRESH("REFRESH", "增量刷新配置"),

    /** 全量覆盖：通知网关节点丢弃旧缓存，重新加载所有配置 */
    OVERWRITE("OVERWRITE", "全量覆盖配置"),

    /** 逻辑下线：标记配置失效，网关应停止对应接口的服务 */
    OFFLINE("OFFLINE", "配置逻辑下线"),

    /** 强制清除：直接从运行态内存中移除相关对象 */
    CLEAR("CLEAR", "强制清除内存");

    /** 事件代码 */
    private final String code;

    /** 事件描述 */
    private final String info;

    /**
     * 根据 code 获取枚举对象
     *
     * @param code 编码
     * @return 匹配的枚举，若无匹配则返回 null
     */
    public static EventActionEnum getByCode(String code) {
        for (EventActionEnum action : EventActionEnum.values()) {
            if (action
                    .getCode()
                    .equals(code)) {
                return action;
            }
        }
        return null;
    }

    /**
     * 校验 code 是否合法
     *
     * @param code 编码
     * @return 是否存在
     */
    public static boolean isValid(String code) {
        return getByCode(code) != null;
    }
}