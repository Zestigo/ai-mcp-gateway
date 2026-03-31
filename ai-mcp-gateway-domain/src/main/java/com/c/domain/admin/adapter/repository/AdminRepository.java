package com.c.domain.admin.adapter.repository;

import com.c.domain.admin.model.entity.*;
import com.c.domain.admin.model.valobj.PageResponse;

import java.util.List;

/**
 * 管理后台领域数据仓储接口
 * 定义网关、工具、协议、鉴权等领域对象的数据持久化操作标准
 *
 * @author cyh
 * @date 2026/03/31
 */
public interface AdminRepository {

    // ==================== 1. 网关配置仓储 ====================

    /**
     * 分页查询网关配置列表
     *
     * @param pageNo   当前页码
     * @param pageSize 每页条数
     * @param keyword  搜索关键词
     * @param status   网关状态
     * @return 分页网关配置数据
     */
    PageResponse<GatewayConfigEntity> queryGatewayConfigPage(int pageNo, int pageSize, String keyword, Integer status);

    /**
     * 根据网关ID查询网关配置详情
     *
     * @param gatewayId 网关唯一标识
     * @return 网关配置实体
     */
    GatewayConfigEntity findGatewayById(String gatewayId);

    /**
     * 创建网关配置
     *
     * @param entity 网关配置实体
     * @return 创建结果
     */
    boolean createGateway(GatewayConfigEntity entity);

    /**
     * 基于CAS乐观锁更新网关配置
     *
     * @param entity     网关配置实体
     * @param oldVersion 旧版本号
     * @return 更新结果
     */
    boolean updateGatewayByCas(GatewayConfigEntity entity, Long oldVersion);

    /**
     * 基于CAS乐观锁更新网关状态
     *
     * @param gatewayId  网关唯一标识
     * @param oldVersion 旧版本号
     * @param status     目标状态
     * @return 更新结果
     */
    boolean updateGatewayStatusByCas(String gatewayId, Long oldVersion, Integer status);

    /**
     * 删除网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 删除结果
     */
    boolean deleteGateway(String gatewayId);

    /**
     * 发布网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 发布结果
     */
    boolean publishGateway(String gatewayId);

    /**
     * 下线网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 下线结果
     */
    boolean offlineGateway(String gatewayId);

    /**
     * 同步网关配置到运行环境
     *
     * @param gatewayId 网关唯一标识
     * @return 同步结果
     */
    boolean syncGatewayConfig(String gatewayId);

    // ==================== 2. 网关工具仓储（三字段唯一：gatewayId + protocolId + toolId） ====================

    /**
     * 分页查询网关工具配置列表
     *
     * @param pageNo     当前页码
     * @param pageSize   每页条数
     * @param gatewayId  网关标识
     * @param protocolId 协议标识
     * @param toolStatus 工具状态
     * @return 分页工具配置数据
     */
    PageResponse<GatewayToolConfigEntity> queryToolPage(int pageNo, int pageSize, String gatewayId, Long protocolId,
                                                        Integer toolStatus);

    /**
     * 根据网关ID查询关联的所有工具配置
     *
     * @param gatewayId 网关唯一标识
     * @return 工具配置列表
     */
    List<GatewayToolConfigEntity> listToolsByGateway(String gatewayId);

    /**
     * 绑定网关与MCP工具
     *
     * @param entity 工具配置实体
     * @return 绑定结果
     */
    boolean bindTool(GatewayToolConfigEntity entity);

    /**
     * 解绑网关与MCP工具
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 解绑结果
     */
    boolean unbindTool(String gatewayId, Integer toolId, Long protocolId);

    /**
     * 校验工具是否已绑定
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 校验结果
     */
    boolean isToolExist(String gatewayId, Integer toolId, Long protocolId);

    /**
     * 基于CAS乐观锁更新工具状态
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @param status     目标状态
     * @param oldVersion 旧版本号
     * @return 更新结果
     */
    boolean updateToolStatusByCas(String gatewayId, Integer toolId, Long protocolId, Integer status, Long oldVersion);

    /**
     * 基于CAS乐观锁更新工具配置
     *
     * @param entity     工具配置实体
     * @param oldVersion 旧版本号
     * @return 更新结果
     */
    boolean updateToolConfigByCas(GatewayToolConfigEntity entity, Long oldVersion);

    // ==================== 3. 协议配置仓储 ====================

    /**
     * 分页查询协议配置列表
     *
     * @param pageNo     当前页码
     * @param pageSize   每页条数
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return 分页协议配置数据
     */
    PageResponse<GatewayProtocolEntity> queryProtocolConfigPage(int pageNo, int pageSize, Long protocolId,
                                                                Integer status);

    /**
     * 查询协议配置列表（不分页）
     *
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return 协议配置列表
     */
    List<GatewayProtocolEntity> getProtocolConfigList(Long protocolId, Integer status);

    /**
     * 根据协议ID查询协议配置详情
     *
     * @param protocolId 协议唯一标识
     * @return 协议配置实体
     */
    GatewayProtocolEntity findByProtocolId(Long protocolId);

    /**
     * 创建协议配置
     *
     * @param entity 协议配置实体
     * @return 创建结果
     */
    boolean createProtocol(GatewayProtocolEntity entity);

    /**
     * 基于CAS乐观锁更新协议配置
     *
     * @param entity     协议配置实体
     * @param oldVersion 旧版本号
     * @return 更新结果
     */
    boolean updateProtocolConfigByCas(GatewayProtocolEntity entity, Long oldVersion);

    /**
     * 根据协议ID删除协议配置
     *
     * @param protocolId 协议唯一标识
     * @return 删除结果
     */
    boolean deleteProtocolById(Long protocolId);

    /**
     * 基于CAS乐观锁更新协议状态
     *
     * @param protocolId 协议唯一标识
     * @param status     目标状态
     * @param oldVersion 旧版本号
     * @return 更新结果
     */
    boolean updateProtocolStatusByCas(Long protocolId, Integer status, Long oldVersion);

    // ==================== 4. 鉴权认证仓储 ====================

    /**
     * 分页查询网关认证信息列表
     *
     * @param pageNo    当前页码
     * @param pageSize  每页条数
     * @param gatewayId 网关唯一标识
     * @return 分页认证数据
     */
    PageResponse<GatewayAuthEntity> queryGatewayAuthPage(int pageNo, int pageSize, String gatewayId);

    /**
     * 根据网关ID查询认证信息列表
     *
     * @param gatewayId 网关唯一标识
     * @return 认证信息列表
     */
    List<GatewayAuthEntity> getAuthListByGatewayId(String gatewayId);

    /**
     * 根据API密钥查询认证信息
     *
     * @param apiKey API访问密钥
     * @return 认证实体
     */
    GatewayAuthEntity findAuthByApiKey(String apiKey);

    /**
     * 校验API密钥是否存在
     *
     * @param apiKey API访问密钥
     * @return 校验结果
     */
    boolean isApiKeyExists(String apiKey);

    /**
     * 保存网关认证信息
     *
     * @param entity 认证实体
     * @return 保存结果
     */
    boolean saveAuth(GatewayAuthEntity entity);

    /**
     * 基于CAS乐观锁更新API密钥状态
     *
     * @param gatewayId  网关标识
     * @param apiKey     API密钥
     * @param status     目标状态
     * @param oldVersion 旧版本号
     * @return 更新结果
     */
    boolean updateApiKeyStatusByCas(String gatewayId, String apiKey, Integer status, Long oldVersion);

    /**
     * 吊销API密钥
     *
     * @param gatewayId 网关标识
     * @param apiKey    API密钥
     * @return 吊销结果
     */
    boolean revokeApiKey(String gatewayId, String apiKey);

}