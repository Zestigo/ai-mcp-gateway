package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpGatewayToolPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 网关工具配置数据访问层
 * 负责网关与工具的绑定、解绑、状态管理、分页查询
 *
 * @author cyh
 * @date 2026/03/31
 */
@Mapper
public interface McpGatewayToolDao {

    /**
     * 分页查询网关工具配置
     *
     * @param offset     偏移量
     * @param pageSize   每页条数
     * @param gatewayId  网关ID
     * @param protocolId 协议ID
     * @param toolStatus 工具状态
     * @return 网关工具PO列表
     */
    List<McpGatewayToolPO> queryGatewayToolConfigPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                                      @Param("gatewayId") String gatewayId,
                                                      @Param("protocolId") Long protocolId,
                                                      @Param("toolStatus") Integer toolStatus);

    /**
     * 统计网关工具配置数量
     *
     * @param gatewayId  网关ID
     * @param protocolId 协议ID
     * @param toolStatus 工具状态
     * @return 记录总数
     */
    long queryGatewayToolConfigCount(@Param("gatewayId") String gatewayId, @Param("protocolId") Long protocolId,
                                     @Param("toolStatus") Integer toolStatus);

    /**
     * 根据网关ID查询绑定的所有工具
     *
     * @param gatewayId 网关ID
     * @return 网关工具PO列表
     */
    List<McpGatewayToolPO> getToolsByGatewayId(@Param("gatewayId") String gatewayId);

    /**
     * 添加网关工具配置
     *
     * @param po 网关工具PO
     * @return 影响行数
     */
    int addToolConfig(McpGatewayToolPO po);

    /**
     * 更新工具配置
     *
     * @param po 网关工具PO
     * @return 影响行数
     */
    int updateToolConfig(McpGatewayToolPO po);

    /**
     * 从网关解绑工具
     *
     * @param gatewayId  网关ID
     * @param protocolId 协议ID
     * @param toolId     工具ID
     * @return 影响行数
     */
    int removeToolFromGateway(@Param("gatewayId") String gatewayId, @Param("protocolId") Long protocolId, @Param(
            "toolId") Long toolId);

    /**
     * 判断工具是否已配置到网关
     *
     * @param gatewayId  网关ID
     * @param protocolId 协议ID
     * @param toolId     工具ID
     * @return 已配置返回大于0，否则0
     */
    int isToolConfigured(@Param("gatewayId") String gatewayId, @Param("protocolId") Long protocolId,
                         @Param("toolId") Long toolId);

    /**
     * 基于乐观锁更新工具状态
     *
     * @param gatewayId  网关ID
     * @param protocolId 协议ID
     * @param toolId     工具ID
     * @param status     目标状态
     * @param oldVersion 乐观锁版本号
     * @return 影响行数
     */
    int updateToolStatusByCas(@Param("gatewayId") String gatewayId, @Param("protocolId") Long protocolId, @Param(
            "toolId") Long toolId, @Param("status") Integer status, @Param("oldVersion") Long oldVersion);

    /**
     * 根据网关ID更新协议信息
     *
     * @param po 网关工具PO
     * @return 影响行数
     */
    int updateProtocolByGatewayId(McpGatewayToolPO po);

    /**
     * 查询工具配置详情
     *
     * @param po 查询条件
     * @return 网关工具PO
     */
    McpGatewayToolPO queryToolConfig(McpGatewayToolPO po);

    /**
     * 根据工具名称查询协议ID
     *
     * @param po 查询条件
     * @return 协议ID
     */
    Long queryToolProtocolIdByToolName(McpGatewayToolPO po);

    /**
     * 基于乐观锁更新工具完整配置
     *
     * @param po         网关工具PO
     * @param oldVersion 乐观锁版本号
     * @return 影响行数
     */
    int updateToolConfigByCas(@Param("po") McpGatewayToolPO po, @Param("oldVersion") Long oldVersion);

}