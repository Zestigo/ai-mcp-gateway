package com.c.api.service;

import com.c.api.common.Response;
import com.c.api.model.dto.*;
import com.c.api.model.request.GatewayConfigRequestDTO;
import com.c.api.model.response.PageResponse;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 管理后台MCP网关开放API服务接口
 * 核心规范：所有更新操作强制使用CAS乐观锁保证并发安全
 *
 * @author cyh
 * @date 2026/03/31
 */
public interface AdminOpenService {

    // ==================== 一、网关管理 ====================

    /**
     * 分页查询网关配置列表
     *
     * @param keyword  搜索关键词（网关名称/标识）
     * @param status   网关状态
     * @param pageNo   当前页码
     * @param pageSize 每页条数
     * @return 分页网关配置数据
     */
    Response<PageResponse<GatewayConfigDTO>> queryGatewayList(String keyword, Integer status, int pageNo, int pageSize);

    /**
     * 创建网关基础配置
     *
     * @param requestDTO 网关配置请求参数
     * @return 创建结果
     */
    Response<Boolean> createGateway(@Valid GatewayConfigRequestDTO.GatewayConfig requestDTO);

    /**
     * 更新网关基础配置（带乐观锁）
     *
     * @param requestDTO 网关配置请求参数
     * @param oldVersion 旧版本号
     * @return 更新结果
     */
    Response<Boolean> updateGateway(@Valid GatewayConfigRequestDTO.GatewayConfig requestDTO, Long oldVersion);

    /**
     * 删除网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 删除结果
     */
    Response<Boolean> deleteGateway(String gatewayId);

    /**
     * 根据网关ID查询网关详情
     *
     * @param gatewayId 网关唯一标识
     * @return 网关配置详情
     */
    Response<GatewayConfigDTO> getGateway(String gatewayId);

    /**
     * 启用网关（带乐观锁）
     *
     * @param gatewayId  网关唯一标识
     * @param oldVersion 旧版本号
     * @return 启用结果
     */
    Response<Boolean> enableGateway(String gatewayId, Long oldVersion);

    /**
     * 禁用网关（带乐观锁）
     *
     * @param gatewayId  网关唯一标识
     * @param oldVersion 旧版本号
     * @return 禁用结果
     */
    Response<Boolean> disableGateway(String gatewayId, Long oldVersion);

    // ==================== 二、协议管理 ====================

    /**
     * 分页查询网关协议配置列表
     *
     * @param protocolId 协议ID
     * @param status     协议状态
     * @param pageNo     当前页码
     * @param pageSize   每页条数
     * @return 分页协议配置数据
     */
    Response<PageResponse<GatewayProtocolDTO>> queryProtocolList(Long protocolId, Integer status, int pageNo,
                                                                 int pageSize);

    /**
     * 查询所有符合条件的网关协议配置
     *
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return 协议配置列表
     */
    Response<List<GatewayProtocolDTO>> listAllProtocols(Long protocolId, Integer status);

    /**
     * 创建HTTP协议配置
     *
     * @param requestDTO 协议配置请求参数
     * @return 创建结果
     */
    Response<Boolean> createProtocolHttp(@Valid GatewayConfigRequestDTO requestDTO);

    /**
     * 更新HTTP协议配置（带乐观锁）
     *
     * @param requestDTO 协议配置请求参数
     * @param oldVersion 旧版本号
     * @return 更新结果
     */
    Response<Boolean> updateProtocolHttp(@Valid GatewayConfigRequestDTO requestDTO, Long oldVersion);

    /**
     * 根据协议ID查询协议详情
     *
     * @param protocolId 协议唯一标识
     * @return 协议配置详情
     */
    Response<GatewayProtocolDTO> getProtocolById(Long protocolId);

    /**
     * 删除协议配置
     *
     * @param protocolId 协议唯一标识
     * @return 删除结果
     */
    Response<Boolean> deleteProtocol(Long protocolId);

    /**
     * 修改协议状态（带乐观锁）
     *
     * @param protocolId 协议唯一标识
     * @param status     目标状态
     * @param oldVersion 旧版本号
     * @return 修改结果
     */
    Response<Boolean> updateProtocolStatus(Long protocolId, Integer status, Long oldVersion);

    // ==================== 三、工具管理 ====================

    /**
     * 分页查询网关工具配置列表
     *
     * @param gatewayId  网关标识
     * @param protocolId 协议标识
     * @param toolStatus 工具状态
     * @param pageNo     当前页码
     * @param pageSize   每页条数
     * @return 分页工具配置数据
     */
    Response<PageResponse<GatewayToolConfigDTO>> queryToolList(String gatewayId, Long protocolId, Integer toolStatus,
                                                               int pageNo, int pageSize);

    /**
     * 根据网关ID查询关联的所有工具配置
     *
     * @param gatewayId 网关唯一标识
     * @return 工具配置列表
     */
    Response<List<GatewayToolConfigDTO>> listToolsByGateway(String gatewayId);

    /**
     * 绑定网关与MCP工具
     *
     * @param requestDTO 工具配置请求参数
     * @return 绑定结果
     */
    Response<Boolean> bindTool(@Valid GatewayConfigRequestDTO.GatewayToolConfig requestDTO);

    /**
     * 更新网关工具配置（带乐观锁）
     *
     * @param requestDTO 工具配置请求参数
     * @param oldVersion 旧版本号
     * @return 更新结果
     */
    Response<Boolean> updateToolConfig(@Valid GatewayConfigRequestDTO.GatewayToolConfig requestDTO, Long oldVersion);

    /**
     * 解绑网关与MCP工具
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 解绑结果
     */
    Response<Boolean> unbindTool(String gatewayId, Integer toolId, Long protocolId);

    /**
     * 校验工具是否已绑定
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @return 校验结果
     */
    Response<Boolean> checkToolExist(String gatewayId, Integer toolId, Long protocolId);

    /**
     * 启用网关工具（带乐观锁）
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @param oldVersion 旧版本号
     * @return 启用结果
     */
    Response<Boolean> enableTool(String gatewayId, Integer toolId, Long protocolId, Long oldVersion);

    /**
     * 禁用网关工具（带乐观锁）
     *
     * @param gatewayId  网关标识
     * @param toolId     工具标识
     * @param protocolId 协议标识
     * @param oldVersion 旧版本号
     * @return 禁用结果
     */
    Response<Boolean> disableTool(String gatewayId, Integer toolId, Long protocolId, Long oldVersion);

    // ==================== 四、网关鉴权 API Key ====================

    /**
     * 分页查询网关认证密钥列表
     *
     * @param gatewayId 网关标识
     * @param pageNo    当前页码
     * @param pageSize  每页条数
     * @return 分页认证数据
     */
    Response<PageResponse<GatewayAuthDTO>> queryGatewayAuthPage(String gatewayId, int pageNo, int pageSize);

    /**
     * 创建网关API访问密钥
     *
     * @param requestDTO 认证配置请求参数
     * @return 创建结果
     */
    Response<Boolean> createApiKey(@Valid GatewayConfigRequestDTO.GatewayAuth requestDTO);

    /**
     * 吊销网关API密钥
     *
     * @param gatewayId 网关标识
     * @param apiKey    API密钥
     * @return 吊销结果
     */
    Response<Boolean> revokeApiKey(String gatewayId, String apiKey);

    /**
     * 启用API密钥（带乐观锁）
     *
     * @param gatewayId  网关标识
     * @param apiKey     API密钥
     * @param oldVersion 旧版本号
     * @return 启用结果
     */
    Response<Boolean> enableApiKey(String gatewayId, String apiKey, Long oldVersion);

    /**
     * 禁用API密钥（带乐观锁）
     *
     * @param gatewayId  网关标识
     * @param apiKey     API密钥
     * @param oldVersion 旧版本号
     * @return 禁用结果
     */
    Response<Boolean> disableApiKey(String gatewayId, String apiKey, Long oldVersion);

    /**
     * 查询API密钥详情
     *
     * @param apiKey API密钥
     * @return 认证详情
     */
    Response<GatewayAuthDTO> getApiKeyDetail(String apiKey);

    /**
     * 校验API密钥是否存在
     *
     * @param apiKey API密钥
     * @return 校验结果
     */
    Response<Boolean> checkApiKeyExist(String apiKey);

    // ==================== 五、网关生命周期 ====================

    /**
     * 发布网关，使配置生效
     *
     * @param gatewayId 网关唯一标识
     * @return 发布结果
     */
    Response<Boolean> publishGateway(String gatewayId);

    /**
     * 下线网关，暂停服务
     *
     * @param gatewayId 网关唯一标识
     * @return 下线结果
     */
    Response<Boolean> offlineGateway(String gatewayId);

    /**
     * 刷新网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 刷新结果
     */
    Response<Boolean> refreshGateway(String gatewayId);
}