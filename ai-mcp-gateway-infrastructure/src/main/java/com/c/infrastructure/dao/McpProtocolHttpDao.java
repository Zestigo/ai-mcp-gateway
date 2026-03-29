package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpProtocolHttpPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * HTTP协议数据访问接口
 *
 * @author cyh
 * @date 2026/03/29
 */
@Mapper
public interface McpProtocolHttpDao {

    // --- 基础CRUD方法 ---

    /**
     * 新增HTTP协议数据
     *
     * @param po HTTP协议持久化对象
     * @return 受影响行数
     */
    int insert(McpProtocolHttpPO po);

    /**
     * 根据主键ID删除HTTP协议数据
     *
     * @param id 主键ID
     * @return 受影响行数
     */
    int deleteById(Long id);

    /**
     * 根据主键ID更新HTTP协议数据
     *
     * @param po HTTP协议持久化对象
     * @return 受影响行数
     */
    int updateById(McpProtocolHttpPO po);

    /**
     * 根据主键ID查询HTTP协议数据
     *
     * @param id 主键ID
     * @return HTTP协议持久化对象
     */
    McpProtocolHttpPO queryById(Long id);

    /**
     * 查询所有HTTP协议数据
     *
     * @return HTTP协议持久化对象集合
     */
    List<McpProtocolHttpPO> queryAll();

    /**
     * 根据协议ID查询HTTP协议数据
     *
     * @param protocolId 协议唯一标识
     * @return HTTP协议持久化对象
     */
    McpProtocolHttpPO queryMcpProtocolHttpByProtocolId(Long protocolId);

    // --- 业务方法 ---

    /**
     * 根据URL和请求方法精确查询协议数据，用于幂等校验
     *
     * @param httpUrl    请求URL
     * @param httpMethod 请求方法
     * @return HTTP协议持久化对象
     */
    McpProtocolHttpPO queryByUrlAndMethod(@Param("httpUrl") String httpUrl, @Param("httpMethod") String httpMethod);

    /**
     * 批量新增HTTP协议数据
     *
     * @param list HTTP协议持久化对象集合
     * @return 受影响行数
     */
    int insertList(List<McpProtocolHttpPO> list);

    /**
     * 根据协议ID物理删除协议数据
     *
     * @param protocolId 协议唯一标识
     * @return 受影响行数
     */
    int deleteByProtocolId(Long protocolId);

    /**
     * 更新协议启用/禁用状态
     *
     * @param protocolId 协议唯一标识
     * @param status     状态编码
     * @return 受影响行数
     */
    int updateStatus(@Param("protocolId") Long protocolId, @Param("status") Integer status);

    /**
     * 分页模糊查询协议列表
     *
     * @param urlKeyword URL模糊关键字
     * @param offset     分页起始位置
     * @param limit      每页条数
     * @return HTTP协议持久化对象集合
     */
    List<McpProtocolHttpPO> queryProtocolPage(@Param("urlKeyword") String urlKeyword, @Param("offset") int offset,
                                              @Param("limit") int limit);

    /**
     * 根据协议ID查询HTTP协议数据
     *
     * @param protocolId 协议唯一标识
     * @return HTTP协议持久化对象
     */
    McpProtocolHttpPO queryByProtocolId(Long protocolId);

    /**
     * 查询所有处于启用状态的协议数据
     * 用于定时任务全量同步或预热
     *
     * @return 启用的协议列表
     */
    List<McpProtocolHttpPO> queryAllActive();
}