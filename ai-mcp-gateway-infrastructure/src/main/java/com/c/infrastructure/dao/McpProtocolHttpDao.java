package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpProtocolHttpPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * HTTP协议数据访问接口
 * 包含 status 字段
 * 全部基于 protocolId 实现
 *
 * @author cyh
 * @date 2026/03/31
 */
@Mapper
public interface McpProtocolHttpDao {

    // ==================== 核心业务方法 ====================

    /**
     * 根据协议ID查询协议配置
     *
     * @param protocolId 协议ID
     * @return HTTP协议PO
     */
    McpProtocolHttpPO findProtocolById(@Param("protocolId") Long protocolId);

    /**
     * 创建HTTP协议配置
     *
     * @param po HTTP协议PO
     * @return 影响行数
     */
    int createProtocol(McpProtocolHttpPO po);

    /**
     * 分页查询协议配置
     *
     * @param offset     偏移量
     * @param pageSize   每页条数
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return HTTP协议PO列表
     */
    List<McpProtocolHttpPO> queryProtocolConfigPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                                    @Param("protocolId") Long protocolId,
                                                    @Param("status") Integer status);

    /**
     * 统计协议配置数量
     *
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return 记录总数
     */
    long queryProtocolConfigCount(@Param("protocolId") Long protocolId, @Param("status") Integer status);

    /**
     * 查询协议配置列表
     *
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return HTTP协议PO列表
     */
    List<McpProtocolHttpPO> getProtocolConfigList(@Param("protocolId") Long protocolId,
                                                  @Param("status") Integer status);

    /**
     * 更新协议状态
     *
     * @param protocolId 协议ID
     * @param status     目标状态
     * @return 影响行数
     */
    int updateStatus(@Param("protocolId") Long protocolId, @Param("status") Integer status);

    /**
     * 根据协议ID查询协议
     *
     * @param protocolId 协议ID
     * @return HTTP协议PO
     */
    McpProtocolHttpPO queryByProtocolId(@Param("protocolId") Long protocolId);

    /**
     * 根据URL和请求方法查询协议
     *
     * @param httpUrl    HTTP地址
     * @param httpMethod 请求方法
     * @return HTTP协议PO
     */
    McpProtocolHttpPO queryByUrlAndMethod(@Param("httpUrl") String httpUrl, @Param("httpMethod") String httpMethod);

    /**
     * 根据协议ID删除协议
     *
     * @param protocolId 协议ID
     * @return 影响行数
     */
    int deleteByProtocolId(@Param("protocolId") Long protocolId);

    /**
     * 根据URL关键词分页查询协议
     *
     * @param urlKeyword URL关键词
     * @param offset     偏移量
     * @param limit      条数
     * @return HTTP协议PO列表
     */
    List<McpProtocolHttpPO> queryProtocolPage(@Param("urlKeyword") String urlKeyword, @Param("offset") int offset,
                                              @Param("limit") int limit);

    /**
     * 查询所有启用状态的协议
     *
     * @return HTTP协议PO列表
     */
    List<McpProtocolHttpPO> queryAllActive();

    /**
     * 基于乐观锁更新协议配置
     *
     * @param po         HTTP协议PO
     * @param oldVersion 乐观锁版本号
     * @return 影响行数
     */
    int updateProtocolConfigByCas(@Param("po") McpProtocolHttpPO po, @Param("oldVersion") Long oldVersion);

    /**
     * 基于乐观锁更新协议状态
     *
     * @param protocolId 协议ID
     * @param status     目标状态
     * @param oldVersion 乐观锁版本号
     * @return 影响行数
     */
    int updateProtocolStatusByCas(@Param("protocolId") Long protocolId, @Param("status") Integer status, @Param(
            "oldVersion") Long oldVersion);

    /**
     * 根据URL关键词统计协议数量
     *
     * @param urlKeyword URL关键词
     * @return 记录总数
     */
    Long countProtocolPage(String urlKeyword);
}