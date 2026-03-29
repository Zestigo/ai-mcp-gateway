package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpSessionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MCP会话数据库访问接口
 *
 * @author cyh
 * @date 2026/03/29
 */
@Mapper
public interface McpSessionDao {

    /**
     * 插入会话记录
     *
     * @param po 会话持久化对象
     */
    void insert(McpSessionPO po);

    /**
     * 根据会话ID查询会话
     *
     * @param sessionId 会话ID
     * @return 会话PO
     */
    McpSessionPO selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 更新会话信息
     *
     * @param po 会话对象
     */
    void update(McpSessionPO po);

    /**
     * 根据会话ID删除会话
     *
     * @param sessionId 会话ID
     */
    void deleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * 统计活跃会话数
     *
     * @return 数量
     */
    long countActiveSessions();

    /**
     * 删除过期会话
     *
     * @return 删除条数
     */
    int deleteExpiredSessions();

    /**
     * 查询全部会话
     *
     * @return 列表
     */
    List<McpSessionPO> selectAll();

    /**
     * 根据网关ID查询会话列表
     *
     * @param gatewayId 网关ID
     * @return 列表
     */
    List<McpSessionPO> selectByGatewayId(@Param("gatewayId") String gatewayId);

    /**
     * 统计会话总数量
     *
     * @return 会话总数
     */
    long countAll();
}