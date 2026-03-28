package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpGatewayAuthPO;
import com.c.infrastructure.dao.po.McpGatewayCompositePO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MCP网关权限数据访问接口
 * 提供网关权限配置的增删改查及聚合联表查询能力
 *
 * @author cyh
 * @date 2026/03/27
 */
@Mapper
public interface McpGatewayAuthDao {

    /**
     * 新增网关权限配置
     *
     * @param po 网关权限持久化对象
     * @return 数据库受影响行数
     */
    int insert(McpGatewayAuthPO po);

    /**
     * 根据主键ID删除网关权限配置
     *
     * @param id 主键ID
     * @return 数据库受影响行数
     */
    int deleteById(Long id);

    /**
     * 根据主键ID更新网关权限配置
     *
     * @param po 网关权限持久化对象
     * @return 数据库受影响行数
     */
    int updateById(McpGatewayAuthPO po);

    /**
     * 根据主键ID查询权限配置
     * 常用于更新操作后的数据校验或回显
     *
     * @param id 主键ID
     * @return 网关权限持久化对象
     */
    McpGatewayAuthPO queryById(Long id);

    /**
     * 查询所有权限配置记录
     * 常用于系统启动时的缓存预热或后台管理列表展示
     *
     * @return 权限配置记录列表
     */
    List<McpGatewayAuthPO> queryAll();

    /**
     * 根据网关唯一标识查询有效权限配置数量
     *
     * @param gatewayId 网关唯一标识
     * @return 有效权限配置数量
     */
    int queryEffectiveGatewayAuthCount(String gatewayId);

    /**
     * 根据网关标识和API密钥查询权限配置
     *
     * @param poReq 网关权限查询请求对象
     * @return 网关权限持久化对象
     */
    McpGatewayAuthPO queryMcpGatewayAuthPO(McpGatewayAuthPO poReq);

    /**
     * 联表查询网关聚合认证信息
     *
     * @param mcpGatewayCompositePO 聚合查询请求对象
     * @return 网关聚合持久化对象
     */
    McpGatewayCompositePO queryCompositeAuth(McpGatewayCompositePO mcpGatewayCompositePO);
}