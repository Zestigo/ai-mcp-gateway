package com.c.domain.protocol.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 协议刷新消息值对象
 * 用于分布式事务消息传递，保证配置刷新一致性
 *
 * @author cyh
 * @date 2026/03/29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolRefreshMessage implements Serializable {

    /** 序列化ID，保证消息跨服务传输兼容性 */
    private static final long serialVersionUID = -1L;

    /** 消息唯一标识，用于幂等去重与状态回写 */
    private String messageId;

    /** 事件类型，标记消息操作类型 */
    private String eventType;

    /** 待刷新协议ID集合，支持批量刷新减少MQ消息量 */
    private List<Long> protocolIds;

    /** 消息时间戳，解决分布式环境消息乱序问题 */
    private Long timestamp;

    /** 消息重试次数，用于指数退避算法 */
    private Integer retryCount;

    /**
     * 校验消息有效性
     *
     * @return 有效返回true，否则false
     */
    public boolean isValid() {
        // 必须同时具备协议ID列表和消息ID才视为合法消息
        return null != protocolIds && !protocolIds.isEmpty() && null != messageId;
    }
}