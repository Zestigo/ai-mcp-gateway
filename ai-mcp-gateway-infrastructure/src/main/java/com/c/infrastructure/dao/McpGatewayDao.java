package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpGatewayPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MCP网关配置数据访问接口
 * 提供网关配置数据的新增、查询、更新、删除等持久化操作
 *
 * @author cyh
 * @date 2026/03/30
 */
@Mapper
public interface McpGatewayDao {

    /**
     * 新增网关配置
     *
     * @param po 网关配置持久化对象
     * @return 受影响的行数
     */
    int insert(McpGatewayPO po);

    /**
     * 根据gatewayId更新网关基础配置（动态更新非空字段）
     *
     * @param po 网关配置持久化对象
     * @return 受影响的行数
     */
    int updateByGatewayId(McpGatewayPO po);

    /**
     * 根据gatewayId更新网关鉴权状态
     *
     * @param po 网关配置持久化对象
     * @return 受影响的行数
     */
    int updateAuthStatusByGatewayId(McpGatewayPO po);

    /**
     * 根据gatewayId查询网关配置
     *
     * @param gatewayId 网关业务ID
     * @return 网关配置持久化对象
     */
    McpGatewayPO queryByGatewayId(@Param("gatewayId") String gatewayId);

    /**
     * 根据物理主键删除网关配置
     *
     * @param id 物理主键ID
     * @return 受影响的行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据物理主键动态更新网关配置
     *
     * @param po 网关配置持久化对象
     * @return 受影响的行数
     */
    int updateById(McpGatewayPO po);

    /**
     * 根据物理主键查询网关配置
     *
     * @param id 物理主键ID
     * @return 网关配置持久化对象
     */
    McpGatewayPO queryById(@Param("id") Long id);

    /**
     * 查询所有网关配置
     *
     * @return 网关配置持久化对象集合
     */
    List<McpGatewayPO> queryAll();

    /**
     * 查询网关总数量
     *
     * @return 网关配置总数量
     */
    long countAll();

    /**
     * 查询所有状态为“启用”的活跃网关 (status=1)
     *
     * @return 活跃网关配置持久化对象集合
     */
    List<McpGatewayPO> queryActiveGateways();

    /**
     * 根据业务主键集合批量查询网关配置
     *
     * @param gatewayIds 网关业务ID集合
     * @return 网关配置持久化对象集合
     */
    List<McpGatewayPO> queryByGatewayIds(@Param("gatewayIds") List<String> gatewayIds);

    /**
     * 根据网关业务ID和版本号更新网关状态（乐观锁控制）
     *
     * @param gatewayId 网关业务ID
     * @param version   旧版本号，用于乐观锁比较
     * @param status    待更新的网关状态值
     * @return 受影响的行数
     */
    int updateStatusWithVersion(@Param("gatewayId") String gatewayId, @Param("version") String version, @Param(
            "status") Integer status);
}