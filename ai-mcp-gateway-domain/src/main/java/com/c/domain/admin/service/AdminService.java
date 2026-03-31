package com.c.domain.admin.service;

import com.c.domain.admin.model.entity.*;
import com.c.domain.admin.model.valobj.PageResponse;

import java.util.List;

/**
 * 管理后台领域服务接口
 * 定义网关、工具、协议、鉴权、运维等核心业务操作标准
 *
 * @author cyh
 * @date 2026/03/31
 */
public interface AdminService {

    // ====================== 1. 网关基础管理 ======================

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
     * 根据网关ID查询配置详情
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
    boolean updateGateway(GatewayConfigEntity entity, String oldVersion);

    /**
     * 基于CAS乐观锁启用网关
     *
     * @param gatewayId  网关唯一标识
     * @param oldVersion 旧版本号
     * @return 启用结果
     */
    boolean enableGateway(String gatewayId, String oldVersion);

    /**
     * 基于CAS乐观锁禁用网关
     *
     * @param gatewayId  网关唯一标识
     * @param oldVersion 旧版本号
     * @return 禁用结果
     */
    boolean disableGateway(String gatewayId, String oldVersion);

    /**
     * 删除网关配置（级联清理关联数据）
     *
     * @param gatewayId 网关唯一标识
     * @return 删除结果
     */
    boolean deleteGateway(String gatewayId);

    // ====================== 2. 工具配置管理 ======================

    /**
     * 分页查询网关工具配置
     *
     * @param pageNo     当前页码
     * @param pageSize   每页条数
     * @param gatewayId  网关标识
     * @param protocolId 协议标识
     * @param toolStatus 工具状态
     * @return 分页工具配置数据
     */
    PageResponse<GatewayToolConfigEntity> queryGatewayToolConfigPage(int pageNo, int pageSize, String gatewayId,
                                                                     Long protocolId, Integer toolStatus);

    /**
     * 根据网关ID查询所有绑定工具
     *
     * @param gatewayId 网关唯一标识
     * @return 工具配置列表
     */
    List<GatewayToolConfigEntity> getToolsByGatewayId(String gatewayId);

    /**
     * 根据三主键查询唯一工具配置
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 工具配置实体
     */
    GatewayToolConfigEntity getToolConfigUnique(String gatewayId, Long toolId, Long protocolId);

    /**
     * 绑定工具到网关
     *
     * @param entity 工具配置实体
     * @return 绑定结果
     */
    boolean addToolConfig(GatewayToolConfigEntity entity);

    /**
     * 基于CAS乐观锁更新工具配置
     *
     * @param entity     工具配置实体
     * @param oldVersion 旧版本号
     * @return 更新结果
     */
    boolean updateToolConfig(GatewayToolConfigEntity entity, String oldVersion);

    /**
     * 基于CAS乐观锁启用工具
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @param oldVersion 旧版本号
     * @return 启用结果
     */
    boolean enableTool(String gatewayId, Long toolId, Long protocolId, String oldVersion);

    /**
     * 基于CAS乐观锁禁用工具
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @param oldVersion 旧版本号
     * @return 禁用结果
     */
    boolean disableTool(String gatewayId, Long toolId, Long protocolId, String oldVersion);

    /**
     * 解绑网关与工具
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 解绑结果
     */
    boolean removeToolFromGateway(String gatewayId, Long toolId, Long protocolId);

    /**
     * 校验工具是否已绑定
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 校验结果
     */
    boolean isToolConfigured(String gatewayId, Long toolId, Long protocolId);

    // ====================== 3. 协议与路由管理 ======================

    /**
     * 分页查询协议配置
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
     * 查询协议配置列表
     *
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return 协议配置列表
     */
    List<GatewayProtocolEntity> getProtocolConfigList(Long protocolId, Integer status);

    /**
     * 根据协议ID查询配置详情
     *
     * @param id 协议唯一标识
     * @return 协议配置实体
     */
    GatewayProtocolEntity findProtocolById(Long id);

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
    boolean updateProtocolConfig(GatewayProtocolEntity entity, String oldVersion);

    /**
     * 根据协议ID删除配置
     *
     * @param id 协议唯一标识
     * @return 删除结果
     */
    boolean deleteProtocolById(Long id);

    /**
     * 根据协议组ID清空配置
     *
     * @param protocolId 协议组ID
     * @return 删除结果
     */
    boolean deleteProtocolsByPId(Long protocolId);

    // ====================== 4. 鉴权管理 ======================

    /**
     * 分页查询API密钥配置
     *
     * @param pageNo    当前页码
     * @param pageSize  每页条数
     * @param gatewayId 网关唯一标识
     * @return 分页认证数据
     */
    PageResponse<GatewayAuthEntity> queryGatewayAuthPage(int pageNo, int pageSize, String gatewayId);

    /**
     * 根据网关ID查询所有API密钥
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
     * 保存认证信息
     *
     * @param entity 认证实体
     * @return 保存结果
     */
    boolean saveAuth(GatewayAuthEntity entity);

    /**
     * 生成安全API密钥并持久化
     *
     * @param gatewayId 网关唯一标识
     * @param rateLimit 访问速率限制
     * @return 生成的API密钥
     */
    String generateAndSaveApiKey(String gatewayId, Integer rateLimit);

    /**
     * 基于CAS乐观锁启用API密钥
     *
     * @param gatewayId  网关标识
     * @param apiKey     API密钥
     * @param oldVersion 旧版本号
     * @return 启用结果
     */
    boolean enableApiKey(String gatewayId, String apiKey, String oldVersion);

    /**
     * 基于CAS乐观锁禁用API密钥
     *
     * @param gatewayId  网关标识
     * @param apiKey     API密钥
     * @param oldVersion 旧版本号
     * @return 禁用结果
     */
    boolean disableApiKey(String gatewayId, String apiKey, String oldVersion);

    /**
     * 吊销API密钥
     *
     * @param gatewayId 网关标识
     * @param apiKey    API密钥
     * @return 吊销结果
     */
    boolean revokeApiKey(String gatewayId, String apiKey);

    // ====================== 5. 系统运维与集群同步 ======================

    /**
     * 发布网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 发布结果
     */
    boolean publishGateway(String gatewayId);

    /**
     * 下线网关服务
     *
     * @param gatewayId 网关唯一标识
     * @return 下线结果
     */
    boolean offlineGateway(String gatewayId);

    /**
     * 同步网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 同步结果
     */
    boolean syncGatewayConfig(String gatewayId);

    /**
     * 清理网关分布式缓存
     *
     * @param gatewayId 网关唯一标识
     */
    void evictGatewayCache(String gatewayId);

}