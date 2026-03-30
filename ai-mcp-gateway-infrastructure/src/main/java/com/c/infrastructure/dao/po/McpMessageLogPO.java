package com.c.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 本地消息记录持久化对象
 *
 * @author cyh
 * @date 2026/03/29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpMessageLogPO {

    /** 自增主键 */
    private Long id;

    /** 消息唯一ID (UUID 或 雪花ID) */
    private String messageId;

    /** 消息主体：通常将 ProtocolRefreshMessage 序列化为 JSON 字符串存储 */
    private String messageData;

    /** 消息状态：WAIT, SUCCESS, FAIL, OVER_LIMIT */
    private String status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 下一次重试时间 */
    private Date nextRetryTime;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}