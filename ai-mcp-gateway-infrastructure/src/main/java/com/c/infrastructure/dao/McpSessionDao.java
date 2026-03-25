package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpSessionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MCP 会话数据访问层
 * 负责会话数据的数据库操作
 *
 * @author cyh
 * @date 2026/03/25
 */
@Mapper
public interface McpSessionDao {

    /**
     * 新增会话记录
     */
    void insert(McpSessionPO po);

    /**
     * 根据会话ID查询会话
     */
    McpSessionPO selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 更新会话信息
     */
    void update(McpSessionPO po);

    /**
     * 根据会话ID删除会话
     */
    void deleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * 统计活跃会话数量
     */
    long countActiveSessions();

    /**
     * 删除已过期的会话
     */
    int deleteExpiredSessions();

    /**
     * 查询所有会话记录
     */
    List<McpSessionPO> selectAll();

    /**
     * 根据网关ID查询会话列表
     */
    List<McpSessionPO> selectByGatewayId(@Param("gatewayId") String gatewayId);
}