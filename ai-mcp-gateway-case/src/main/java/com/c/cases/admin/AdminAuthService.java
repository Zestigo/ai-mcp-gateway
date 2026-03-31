package com.c.cases.admin;

import com.c.domain.admin.model.entity.GatewayAuthEntity;
import com.c.domain.admin.model.valobj.PageResponse;

/**
 * 管理后台网关认证服务接口
 * 定义网关API密钥相关的业务操作标准
 *
 * @author cyh
 * @date 2026/03/31
 */
public interface AdminAuthService {

    /**
     * 分页查询网关认证信息列表
     *
     * @param pageNo    当前页码
     * @param pageSize  每页条数
     * @param gatewayId 网关唯一标识
     * @return 分页网关认证数据
     */
    PageResponse<GatewayAuthEntity> queryGatewayAuthPage(int pageNo, int pageSize, String gatewayId);

    /**
     * 保存网关认证信息
     *
     * @param entity 网关认证实体对象
     * @return 保存结果
     */
    boolean saveAuth(GatewayAuthEntity entity);

    /**
     * 吊销网关API密钥
     *
     * @param gatewayId 网关唯一标识
     * @param apiKey    API访问密钥
     * @return 吊销结果
     */
    boolean revokeApiKey(String gatewayId, String apiKey);

    /**
     * 根据API密钥查询认证详情
     *
     * @param apiKey API访问密钥
     * @return 网关认证实体
     */
    GatewayAuthEntity findAuthByApiKey(String apiKey);

    /**
     * 判断API密钥是否已存在
     *
     * @param apiKey API访问密钥
     * @return 存在结果
     */
    boolean isApiKeyExists(String apiKey);

    /**
     * 启用网关API密钥（带CAS乐观锁）
     *
     * @param gatewayId  网关唯一标识
     * @param apiKey     API访问密钥
     * @param oldVersion 乐观锁旧版本号
     * @return 启用结果
     */
    boolean enableApiKey(String gatewayId, String apiKey, Long oldVersion);

    /**
     * 禁用网关API密钥（带CAS乐观锁）
     *
     * @param gatewayId  网关唯一标识
     * @param apiKey     API访问密钥
     * @param oldVersion 乐观锁旧版本号
     * @return 禁用结果
     */
    boolean disableApiKey(String gatewayId, String apiKey, Long oldVersion);

}