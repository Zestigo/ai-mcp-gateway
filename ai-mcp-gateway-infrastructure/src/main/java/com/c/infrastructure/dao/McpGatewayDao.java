package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpGatewayPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MCP网关配置数据访问接口
 * 提供基于物理主键和业务主键的双维度持久化操作
 *
 * @author cyh
 * @date 2026/03/29
 */
@Mapper
public interface McpGatewayDao {

    /**
     * 新增网关配置
     *
     * @param po 网关配置持久化对象
     * @return 插入行数
     */
    int insert(McpGatewayPO po);

    /**
     * 根据gatewayId更新网关基础配置
     *
     * @param po 网关配置持久化对象
     * @return 影响行数
     */
    int updateByGatewayId(McpGatewayPO po);

    /**
     * 根据gatewayId更新网关鉴权状态
     *
     * @param po 网关配置持久化对象
     * @return 影响行数
     */
    int updateAuthStatusByGatewayId(McpGatewayPO po);

    /**
     * 根据gatewayId查询网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 网关配置对象
     */
    McpGatewayPO queryByGatewayId(@Param("gatewayId") String gatewayId);

    /**
     * 根据物理主键删除网关配置
     *
     * @param id 物理主键
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据物理主键动态更新网关配置
     *
     * @param po 网关配置持久化对象
     * @return 影响行数
     */
    int updateById(McpGatewayPO po);

    /**
     * 根据物理主键查询网关配置
     *
     * @param id 物理主键
     * @return 网关配置对象
     */
    McpGatewayPO queryById(@Param("id") Long id);

    /**
     * 查询所有网关配置
     *
     * @return 网关配置列表
     */
    List<McpGatewayPO> queryAll();

    /**
     * 查询网关总数量
     *
     * @return 总行数
     */
    long countAll();

}