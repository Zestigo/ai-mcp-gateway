package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpGatewayPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 网关基础配置数据访问层
 * 负责网关信息的增删改查、状态管理、乐观锁更新
 *
 * @author cyh
 * @date 2026/03/31
 */
@Mapper
public interface McpGatewayDao {

    /**
     * 分页查询网关配置列表
     *
     * @param offset   偏移量
     * @param pageSize 每页条数
     * @param keyword  搜索关键词
     * @param status   网关状态
     * @return 网关配置PO列表
     */
    List<McpGatewayPO> queryGatewayConfigPage(@Param("offset") int offset, @Param("pageSize") int pageSize, @Param(
            "keyword") String keyword, @Param("status") Integer status);

    /**
     * 统计网关配置总数
     *
     * @param keyword 搜索关键词
     * @param status  网关状态
     * @return 记录总数
     */
    long queryGatewayConfigCount(@Param("keyword") String keyword, @Param("status") Integer status);

    /**
     * 根据网关ID查询网关配置
     *
     * @param gatewayId 网关ID
     * @return 网关配置PO
     */
    McpGatewayPO findGatewayById(@Param("gatewayId") String gatewayId);

    /**
     * 插入网关配置
     *
     * @param po 网关配置PO
     * @return 影响行数
     */
    int insertGateway(McpGatewayPO po);

    /**
     * 基于乐观锁更新网关状态
     *
     * @param gatewayId  网关ID
     * @param oldVersion 乐观锁版本号
     * @param status     目标状态
     * @return 影响行数
     */
    int updateGatewayStatusByCas(@Param("gatewayId") String gatewayId, @Param("oldVersion") Long oldVersion, @Param(
            "status") Integer status);

    /**
     * 根据网关ID删除网关配置
     *
     * @param gatewayId 网关ID
     * @return 影响行数
     */
    int deleteGateway(@Param("gatewayId") String gatewayId);

    /**
     * 发布网关
     *
     * @param gatewayId 网关ID
     * @return 影响行数
     */
    int publishGateway(@Param("gatewayId") String gatewayId);

    /**
     * 下线网关
     *
     * @param gatewayId 网关ID
     * @return 影响行数
     */
    int offlineGateway(@Param("gatewayId") String gatewayId);

    /**
     * 根据主键ID删除网关
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据主键ID更新网关
     *
     * @param po 网关配置PO
     * @return 影响行数
     */
    int updateById(McpGatewayPO po);

    /**
     * 根据网关ID更新网关
     *
     * @param po 网关配置PO
     * @return 影响行数
     */
    int updateByGatewayId(McpGatewayPO po);

    /**
     * 根据主键ID查询网关
     *
     * @param id 主键ID
     * @return 网关配置PO
     */
    McpGatewayPO queryById(@Param("id") Long id);

    /**
     * 根据网关ID查询网关
     *
     * @param gatewayId 网关ID
     * @return 网关配置PO
     */
    McpGatewayPO queryByGatewayId(@Param("gatewayId") String gatewayId);

    /**
     * 查询所有网关配置
     *
     * @return 网关配置PO列表
     */
    List<McpGatewayPO> queryAll();

    /**
     * 根据网关ID列表批量查询网关
     *
     * @param gatewayIds 网关ID集合
     * @return 网关配置PO列表
     */
    List<McpGatewayPO> queryByGatewayIds(@Param("gatewayIds") List<String> gatewayIds);

    /**
     * 根据网关ID更新认证状态
     *
     * @param po 网关配置PO
     * @return 影响行数
     */
    int updateAuthStatusByGatewayId(McpGatewayPO po);

    /**
     * 基于乐观锁更新网关完整配置
     *
     * @param po         网关配置PO
     * @param oldVersion 乐观锁版本号
     * @return 影响行数
     */
    int updateGatewayByCas(@Param("po") McpGatewayPO po, @Param("oldVersion") Long oldVersion);

}