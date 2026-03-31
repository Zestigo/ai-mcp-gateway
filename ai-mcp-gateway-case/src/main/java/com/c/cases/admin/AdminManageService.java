package com.c.cases.admin;

/**
 * 管理后台网关生命周期管理服务接口
 * 定义网关发布、下线、配置同步等生命周期操作标准
 *
 * @author cyh
 * @date 2026/03/31
 */
public interface AdminManageService {

    /**
     * 发布网关，使配置生效
     *
     * @param gatewayId 网关唯一标识
     * @return 发布结果
     */
    boolean publishGateway(String gatewayId);

    /**
     * 下线网关，暂停服务
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

}