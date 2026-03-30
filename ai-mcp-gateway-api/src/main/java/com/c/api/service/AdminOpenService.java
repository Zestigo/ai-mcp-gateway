package com.c.api.service;

import com.c.api.common.Response;
import com.c.api.model.dto.GatewayConfigDTO;
import com.c.api.model.request.GatewayConfigRequestDTO;
import com.c.api.model.response.GatewayConfigResponseDTO;

import java.util.List;

/**
 * 运营配置管理服务接口
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface AdminOpenService {

    /**
     * 保存网关基础配置
     *
     * @param requestDTO 网关配置请求参数
     * @return 响应结果
     */
    Response<GatewayConfigResponseDTO> saveGatewayConfig(GatewayConfigRequestDTO.GatewayConfig requestDTO);

    /**
     * 保存网关工具配置
     *
     * @param requestDTO 网关工具配置请求参数
     * @return 响应结果
     */
    Response<GatewayConfigResponseDTO> saveGatewayToolConfig(GatewayConfigRequestDTO.GatewayToolConfig requestDTO);

    /**
     * 保存网关协议配置
     *
     * @param requestDTO 网关协议配置请求参数
     * @return 响应结果
     */
    Response<GatewayConfigResponseDTO> saveGatewayProtocol(GatewayConfigRequestDTO.GatewayProtocol requestDTO);

    /**
     * 保存网关认证配置
     *
     * @param requestDTO 网关认证配置请求参数
     * @return 响应结果
     */
    Response<GatewayConfigResponseDTO> saveGatewayAuth(GatewayConfigRequestDTO.GatewayAuth requestDTO);

    /**
     * 查询网关配置列表
     *
     * @return 网关配置列表
     */
    Response<List<GatewayConfigDTO>> queryGatewayConfigList();

}