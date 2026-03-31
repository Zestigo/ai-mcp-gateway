package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpGatewayAuthPO;
import com.c.infrastructure.dao.po.McpGatewayCompositePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 网关认证配置数据访问层
 * 负责网关API密钥、认证信息的数据库操作
 *
 * @author cyh
 * @date 2026/03/31
 */
@Mapper
public interface McpGatewayAuthDao {

    /**
     * 分页查询网关认证配置列表
     *
     * @param offset    分页偏移量
     * @param pageSize  每页条数
     * @param gatewayId 网关ID
     * @return 认证配置PO列表
     */
    List<McpGatewayAuthPO> queryGatewayAuthPage(@Param("offset") int offset, @Param("pageSize") int pageSize, @Param(
            "gatewayId") String gatewayId);

    /**
     * 统计网关认证配置总数
     *
     * @param gatewayId 网关ID
     * @return 记录总数
     */
    long queryGatewayAuthCount(@Param("gatewayId") String gatewayId);

    /**
     * 根据网关ID查询所有认证配置
     *
     * @param gatewayId 网关ID
     * @return 认证配置PO列表
     */
    List<McpGatewayAuthPO> getAuthListByGatewayId(@Param("gatewayId") String gatewayId);

    /**
     * 根据API密钥查询认证配置
     *
     * @param apiKey API密钥
     * @return 认证配置PO
     */
    McpGatewayAuthPO findAuthByApiKey(@Param("apiKey") String apiKey);

    /**
     * 判断API密钥是否已存在
     *
     * @param apiKey API密钥
     * @return 存在返回大于0，不存在返回0
     */
    int isApiKeyExists(@Param("apiKey") String apiKey);

    /**
     * 保存网关认证配置
     *
     * @param po 认证配置PO
     * @return 影响行数
     */
    int saveAuth(McpGatewayAuthPO po);

    /**
     * 基于乐观锁更新API密钥状态
     *
     * @param gatewayId  网关ID
     * @param apiKey     API密钥
     * @param status     目标状态
     * @param oldVersion 乐观锁版本号
     * @return 影响行数
     */
    int updateApiKeyStatusByCas(@Param("gatewayId") String gatewayId, @Param("apiKey") String apiKey,
                                @Param("status") Integer status, @Param("oldVersion") Long oldVersion);

    /**
     * 吊销API密钥
     *
     * @param gatewayId 网关ID
     * @param apiKey    API密钥
     * @return 影响行数
     */
    int revokeApiKey(@Param("gatewayId") String gatewayId, @Param("apiKey") String apiKey);

    /**
     * 查询网关有效授权数量
     *
     * @param gatewayId 网关ID
     * @return 有效授权数量
     */
    int queryEffectiveGatewayAuthCount(@Param("gatewayId") String gatewayId);

    /**
     * 条件查询网关认证配置
     *
     * @param poReq 查询条件PO
     * @return 认证配置PO
     */
    McpGatewayAuthPO queryMcpGatewayAuthPO(McpGatewayAuthPO poReq);

    /**
     * 查询网关综合认证信息（关联网关+认证表）
     *
     * @param mcpGatewayCompositePO 查询条件
     * @return 综合认证PO
     */
    McpGatewayCompositePO queryCompositeAuth(McpGatewayCompositePO mcpGatewayCompositePO);

}