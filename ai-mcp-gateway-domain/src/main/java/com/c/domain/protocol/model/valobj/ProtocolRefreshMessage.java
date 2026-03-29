package com.c.domain.protocol.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

/**
 * 协议刷新消息值对象
 * 用于Redis广播传递协议刷新指令
 *
 * @author cyh
 * @date 2026/03/29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolRefreshMessage implements Serializable {

    /** 事件类型 */
    private String eventType;

    /** 需要刷新的协议ID集合 */
    private List<Long> protocolIds;

    /** 消息时间戳 */
    private Long timestamp;
}